package com.ignaciovalero.saludario.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignaciovalero.saludario.SaludarioApplication
import com.ignaciovalero.saludario.core.DoseConstants
import com.ignaciovalero.saludario.data.local.entity.MedicationLogEntity
import com.ignaciovalero.saludario.data.local.entity.MedicationStatus
import com.ignaciovalero.saludario.data.work.AppWorkScheduler
import com.ignaciovalero.saludario.domain.repository.MedicationLogRepository
import com.ignaciovalero.saludario.domain.repository.MedicationRepository
import com.ignaciovalero.saludario.domain.scheduling.ScheduledDoseGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class TodayViewModel(
    private val medicationRepository: MedicationRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val workScheduler: AppWorkScheduler,
    private val clock: Clock
) : ViewModel() {

    private val _currentDate = MutableStateFlow(nowDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    /**
     * Dosis solicitada por una notificación a la que la UI debe hacer scroll
     * y resaltar. Permanece hasta que la pantalla la consume vía
     * [clearHighlightedDose].
     */
    private val _highlightedDose = MutableStateFlow<HighlightedDose?>(null)

    /**
     * Mes que el calendario está mostrando. Cambiar de mes en el calendario
     * no debe modificar la fecha activa, por eso está separado de
     * [_currentDate].
     */
    private val _visibleCalendarMonth = MutableStateFlow(YearMonth.from(nowDate()))

    // Eventos one-shot para mostrar Snackbars con "Deshacer".
    private val _snackbarEvents = Channel<TodaySnackbarEvent>(Channel.BUFFERED)
    val snackbarEvents = _snackbarEvents.receiveAsFlow()

    // Último cambio reversible (sobrescribe al anterior). Permite un único
    // nivel de "deshacer", suficiente para acciones inmediatas.
    private var lastUndoable: UndoableAction? = null

    val uiState: StateFlow<TodayUiState> =
        combine(
            combine(
                _currentDate,
                medicationRepository.observeAll(),
                medicationLogRepository.observeAll()
            ) { date, meds, logs -> Triple(date, meds, logs) },
            _highlightedDose,
            _visibleCalendarMonth
        ) { (selectedDate, medications, logs), highlighted, visibleMonth ->
            val today = nowDate()
            val now = nowDateTime()
            val generator = ScheduledDoseGenerator(
                medications = medications,
                logs = logs,
                now = now
            )
            val items = generator.generateDosesForDate(selectedDate)

            val streak = computeStreakDays(
                generator = generator,
                today = today
            )

            val calendarStatuses = computeMonthCalendarStatuses(
                generator = generator,
                month = visibleMonth
            )

            TodayUiState(
                selectedDate = selectedDate,
                scheduledItems = items,
                canModifyIntake = selectedDate == today,
                streakDays = streak,
                hasAnyMedication = medications.isNotEmpty(),
                highlightedDose = highlighted,
                visibleCalendarMonth = visibleMonth,
                calendarDayStatuses = calendarStatuses
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /**
     * Calcula el estado de medicación para los 42 días que normalmente
     * componen un grid mensual (6 semanas empezando en lunes), de forma que
     * el calendario tenga datos también para los días "de relleno" del mes
     * anterior y siguiente.
     */
    private fun computeMonthCalendarStatuses(
        generator: ScheduledDoseGenerator,
        month: YearMonth
    ): Map<LocalDate, CalendarDayMedicationStatus> {
        val firstOfMonth = month.atDay(1)
        val daysFromMonday = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val gridStart = firstOfMonth.minusDays(daysFromMonday.toLong())
        val result = LinkedHashMap<LocalDate, CalendarDayMedicationStatus>(CALENDAR_GRID_DAYS)
        for (offset in 0 until CALENDAR_GRID_DAYS) {
            val day = gridStart.plusDays(offset.toLong())
            val doses = generator.generateDosesForDate(day)
            result[day] = CalendarDayMedicationStatus.fromDoses(day, doses)
        }
        return result
    }

    private fun computeStreakDays(
        generator: ScheduledDoseGenerator,
        today: LocalDate
    ): Int {
        // Cuenta días consecutivos hacia atrás con 100% de adherencia.
        // - Hoy cuenta solo si todas las dosis programadas están tomadas.
        // - Días sin dosis programadas no rompen ni suman a la racha.
        // - Limitado a STREAK_LOOKBACK_DAYS para acotar cálculo.
        // Reutilizamos el mismo generador (es inmutable y stateless por
        // día) para evitar instanciarlo en cada iteración.
        var streak = 0
        var date = today
        repeat(STREAK_LOOKBACK_DAYS) {
            val items = generator.generateDosesForDate(date)
            when {
                items.isEmpty() -> { /* día neutro: no suma ni rompe */ }
                items.all { it.isTaken } -> streak += 1
                else -> return streak
            }
            date = date.minusDays(1)
        }
        return streak
    }

    fun previousDay() {
        val newDate = _currentDate.value.minusDays(1)
        _currentDate.value = newDate
        _visibleCalendarMonth.value = YearMonth.from(newDate)
    }

    fun nextDay() {
        val newDate = _currentDate.value.plusDays(1)
        _currentDate.value = newDate
        _visibleCalendarMonth.value = YearMonth.from(newDate)
    }

    fun setDate(date: LocalDate) {
        _currentDate.value = date
        _visibleCalendarMonth.value = YearMonth.from(date)
    }

    fun goToToday() {
        val today = nowDate()
        _currentDate.value = today
        _visibleCalendarMonth.value = YearMonth.from(today)
    }

    /**
     * Cambia el mes mostrado por el calendario sin alterar la fecha
     * seleccionada. Así el usuario puede explorar otros meses antes de
     * decidir.
     */
    fun setVisibleCalendarMonth(month: YearMonth) {
        _visibleCalendarMonth.value = month
    }

    fun previousCalendarMonth() {
        _visibleCalendarMonth.value = _visibleCalendarMonth.value.minusMonths(1)
    }

    fun nextCalendarMonth() {
        _visibleCalendarMonth.value = _visibleCalendarMonth.value.plusMonths(1)
    }

    /**
     * Abre la pantalla en la fecha de la dosis indicada y deja registrado el
     * objetivo a destacar. La UI hará scroll automáticamente cuando la dosis
     * aparezca en `scheduledItems`. Si la dosis pertenece a otro día, se
     * cambia la fecha (el día queda en modo solo-lectura si no es hoy).
     */
    fun openDoseFromNotification(medicationId: Long, scheduledDateTime: LocalDateTime) {
        _currentDate.value = scheduledDateTime.toLocalDate()
        _visibleCalendarMonth.value = YearMonth.from(scheduledDateTime.toLocalDate())
        _highlightedDose.value = HighlightedDose(
            medicationId = medicationId,
            originalScheduledTime = scheduledDateTime.toLocalTime()
        )
    }

    /**
     * Llamado por la UI tras consumir el highlight (típicamente después del
     * primer scroll), para evitar que se repita en futuras recomposiciones.
     */
    fun clearHighlightedDose() {
        _highlightedDose.value = null
    }

    fun toggleTaken(medicationId: Long, time: String) {
        viewModelScope.launch {
            val today = nowDate()
            val selectedDate = _currentDate.value
            if (selectedDate != today) return@launch

            val scheduledDateTime = LocalDateTime.of(selectedDate, java.time.LocalTime.parse(time))

            val currentItems = uiState.value.scheduledItems
            val item = currentItems.find { it.medicationId == medicationId && it.time == time } ?: return@launch

            if (item.logId != null) {
                val log = medicationLogRepository.getById(item.logId) ?: return@launch
                val wasTaken = log.status == MedicationStatus.TAKEN
                val newStatus = if (wasTaken) MedicationStatus.PENDING else MedicationStatus.TAKEN
                val newTakenTime = if (newStatus == MedicationStatus.TAKEN) nowDateTime() else null
                medicationLogRepository.update(log.copy(status = newStatus, takenTime = newTakenTime))
                if (newStatus == MedicationStatus.TAKEN && !wasTaken) {
                    medicationRepository.decreaseStockForTakenDose(medicationId)
                }
                lastUndoable = UndoableAction.UpdateLog(log)
                _snackbarEvents.trySend(
                    if (newStatus == MedicationStatus.TAKEN) TodaySnackbarEvent.MARKED_TAKEN
                    else TodaySnackbarEvent.UNMARKED_TAKEN
                )
            } else {
                val newId = medicationLogRepository.insert(
                    MedicationLogEntity(
                        medicationId = medicationId,
                        scheduledTime = scheduledDateTime,
                        takenTime = nowDateTime(),
                        status = MedicationStatus.TAKEN
                    )
                )
                medicationRepository.decreaseStockForTakenDose(medicationId)
                lastUndoable = UndoableAction.InsertLog(newId, medicationId)
                _snackbarEvents.trySend(TodaySnackbarEvent.MARKED_TAKEN)
            }
        }
    }

    private fun nowDate(): LocalDate = LocalDate.now(clock)

    private fun nowDateTime(): LocalDateTime = LocalDateTime.now(clock)

    fun postpone(medicationId: Long, time: String, minutes: Long = DoseConstants.POSTPONE_MINUTES) {
        viewModelScope.launch {
            val today = nowDate()
            val selectedDate = _currentDate.value
            if (selectedDate != today) return@launch

            val currentItems = uiState.value.scheduledItems
            val item = currentItems.find { it.medicationId == medicationId && it.time == time } ?: return@launch
            // No tiene sentido posponer una dosis ya tomada.
            if (item.isTaken) return@launch

            val postponeMinutes = minutes.coerceAtLeast(1L)
            val postponedUntil = nowDateTime().plusMinutes(postponeMinutes)
            val originalScheduled = item.scheduledAt

            if (item.logId != null) {
                val log = medicationLogRepository.getById(item.logId) ?: return@launch
                medicationLogRepository.update(
                    log.copy(
                        status = MedicationStatus.POSTPONED,
                        postponedUntil = postponedUntil,
                        takenTime = null
                    )
                )
                lastUndoable = UndoableAction.UpdateLog(log)
            } else {
                val newId = medicationLogRepository.insert(
                    MedicationLogEntity(
                        medicationId = medicationId,
                        scheduledTime = originalScheduled,
                        status = MedicationStatus.POSTPONED,
                        postponedUntil = postponedUntil
                    )
                )
                lastUndoable = UndoableAction.InsertLog(newId, medicationId)
            }

            // Cancelar el recordatorio original (si lo hubiera) y programar el
            // nuevo conservando la `scheduledTime` original para que la pr\u00f3xima
            // notificaci\u00f3n coincida con el log existente.
            workScheduler.cancelReminderForScheduledTime(medicationId, originalScheduled)
            workScheduler.schedulePostponed(
                medicationId = medicationId,
                postponeMinutes = postponeMinutes,
                scheduledTimeOriginal = originalScheduled
            )
            _snackbarEvents.trySend(TodaySnackbarEvent.postponed(postponeMinutes))
        }
    }

    /**
     * Revierte la \u00faltima acci\u00f3n reversible (toggleTaken / postpone).
     */
    fun undoLastAction() {
        val action = lastUndoable ?: return
        lastUndoable = null
        viewModelScope.launch {
            when (action) {
                is UndoableAction.UpdateLog -> {
                    val current = medicationLogRepository.getById(action.original.id) ?: return@launch
                    // Nota: por simplicidad, el undo no revierte ajustes de stock.
                    // El log vuelve a su estado anterior, pero el stock no se incrementa.
                    medicationLogRepository.update(action.original)
                }
                is UndoableAction.InsertLog -> {
                    val current = medicationLogRepository.getById(action.logId) ?: return@launch
                    medicationLogRepository.delete(current)
                }
            }
        }
    }

    companion object {
        private const val CALENDAR_GRID_DAYS = 42
        private const val STREAK_LOOKBACK_DAYS = 60

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SaludarioApplication
                TodayViewModel(
                    app.container.medicationRepository,
                    app.container.medicationLogRepository,
                    app.container.workScheduler,
                    Clock.systemDefaultZone()
                )
            }
        }
    }
}
