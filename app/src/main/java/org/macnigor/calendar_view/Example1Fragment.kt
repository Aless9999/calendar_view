package org.macnigor.calendar_view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.*
import androidx.fragment.app.Fragment
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.*
import org.macnigor.calendar_view.databinding.Example1FragmentBinding
import org.macnigor.calendar_view.databinding.Example1CalendarDayBinding
import org.macnigor.calendar_view.shared.displayText
import org.macnigor.calendar_view.shared.setTextColorRes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class Example1Fragment : Fragment(R.layout.example_1_fragment) {
    private lateinit var noteStorage: NoteStorage

    private lateinit var binding: Example1FragmentBinding
    private val selectedDates = mutableSetOf<LocalDate>()
    private val today = LocalDate.now()

    private val monthCalendarView get() = binding.exOneCalendar
    private val weekCalendarView get() = binding.exOneWeekCalendar

    private val notesMap = mutableMapOf<LocalDate, String>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = Example1FragmentBinding.bind(view)
        applyInsets(binding)

        noteStorage = NoteStorage(requireContext())
        notesMap.putAll(noteStorage.loadNotes())


        val daysOfWeek = daysOfWeek()

        binding.legendLayout.root.children
            .filterIsInstance<TextView>()
            .forEachIndexed { index, tv ->
                tv.text = daysOfWeek[index].displayText()
                tv.setTextColorRes(R.color.example_1_white)
            }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        setupMonthCalendar(startMonth, endMonth, currentMonth, daysOfWeek)
        setupWeekCalendar(startMonth, endMonth, currentMonth, daysOfWeek)

        monthCalendarView.isInvisible = binding.weekModeCheckBox.isChecked
        weekCalendarView.isInvisible = !binding.weekModeCheckBox.isChecked

        binding.weekModeCheckBox.setOnCheckedChangeListener(weekModeToggled)
    }

    private fun setupMonthCalendar(startMonth: YearMonth, endMonth: YearMonth, currentMonth: YearMonth, daysOfWeek: List<DayOfWeek>) {
        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay
            val textView = Example1CalendarDayBinding.bind(view).exOneDayText

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        dateClicked(day.date)
                    }
                }
            }
        }

        monthCalendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                bindDate(day.date, container.textView, day.position == DayPosition.MonthDate)
            }
        }

        monthCalendarView.monthScrollListener = { updateTitle() }
        monthCalendarView.setup(startMonth, endMonth, daysOfWeek.first())
        monthCalendarView.scrollToMonth(currentMonth)
    }

    private fun setupWeekCalendar(startMonth: YearMonth, endMonth: YearMonth, currentMonth: YearMonth, daysOfWeek: List<DayOfWeek>) {
        class WeekDayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: WeekDay
            val textView = Example1CalendarDayBinding.bind(view).exOneDayText

            init {
                view.setOnClickListener {
                    if (day.position == WeekDayPosition.RangeDate) {
                        dateClicked(day.date)
                    }
                }
            }
        }

        weekCalendarView.dayBinder = object : WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View) = WeekDayViewContainer(view)
            override fun bind(container: WeekDayViewContainer, day: WeekDay) {
                container.day = day
                bindDate(day.date, container.textView, day.position == WeekDayPosition.RangeDate)
            }
        }

        weekCalendarView.weekScrollListener = { updateTitle() }
        weekCalendarView.setup(startMonth.atStartOfMonth(), endMonth.atEndOfMonth(), daysOfWeek.first())
        weekCalendarView.scrollToWeek(currentMonth.atStartOfMonth())
    }

    private fun bindDate(date: LocalDate, textView: TextView, isSelectable: Boolean) {
        textView.text = date.dayOfMonth.toString()

        when {
            !isSelectable -> {
                textView.setTextColorRes(R.color.example_1_white_light)
                textView.background = null
            }
            notesMap.containsKey(date) -> { // Заметка есть
                textView.setTextColorRes(R.color.white)
                textView.setBackgroundResource(R.drawable.example_1_note_bg) // добавим новый фон
            }
            selectedDates.contains(date) -> {
                textView.setTextColorRes(R.color.example_1_bg)
                textView.setBackgroundResource(R.drawable.example_1_selected_bg)
            }
            today == date -> {
                textView.setTextColorRes(R.color.example_1_white)
                textView.setBackgroundResource(R.drawable.example_1_today_bg)
            }
            else -> {
                textView.setTextColorRes(R.color.example_1_white)
                textView.background = null
            }
        }

    }


    private fun dateClicked(date: LocalDate) {
        val context = requireContext()
        val existingNote = notesMap[date]

        if (existingNote != null) {
            // Есть заметка – покажем выбор
            AlertDialog.Builder(context)
                .setTitle("Заметка на $date")
                .setMessage(existingNote)
                .setPositiveButton("Изменить") { _, _ ->
                    showNoteDialog(date, existingNote)
                }
                .setNegativeButton("Удалить") { _, _ ->
                    notesMap.remove(date)
                    selectedDates.remove(date)
                    monthCalendarView.notifyCalendarChanged()
                    weekCalendarView.notifyCalendarChanged()
                    noteStorage.saveNotes(notesMap)

                }
                .setNeutralButton("Закрыть", null)
                .show()
        } else {
            // Нет заметки – откроем диалог ввода
            showNoteDialog(date)
        }
    }

    private fun showNoteDialog(date: LocalDate, prefill: String = "") {
        val context = requireContext()

        val editText = EditText(context).apply {
            setText(prefill)
            hint = "Введите заметку"
        }

        AlertDialog.Builder(context)
            .setTitle("Заметка на $date")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val note = editText.text.toString()
                if (note.isNotBlank()) {
                    notesMap[date] = note
                    selectedDates.add(date)
                } else {
                    notesMap.remove(date)
                    selectedDates.remove(date)
                }
                monthCalendarView.notifyCalendarChanged()
                weekCalendarView.notifyCalendarChanged()

                noteStorage.saveNotes(notesMap)// сохраняем в базе



            }
            .setNegativeButton("Отмена", null)
            .show()
    }




    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        if (!binding.weekModeCheckBox.isChecked) {
            val month = monthCalendarView.findFirstVisibleMonth()?.yearMonth ?: return
            binding.exOneYearText.text = month.year.toString()
            binding.exOneMonthText.text = month.month.displayText(false)
        } else {
            val week = weekCalendarView.findFirstVisibleWeek() ?: return
            val first = week.days.first().date
            val last = week.days.last().date

            if (first.yearMonth == last.yearMonth) {
                binding.exOneYearText.text = first.year.toString()
                binding.exOneMonthText.text = first.month.displayText(false)
            } else {
                binding.exOneMonthText.text = "${first.month.displayText(false)} - ${last.month.displayText(false)}"
                binding.exOneYearText.text = if (first.year == last.year) "${first.year}" else "${first.year} - ${last.year}"
            }
        }
    }

    private val weekModeToggled = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        val date = if (isChecked) {
            monthCalendarView.findFirstVisibleDay()?.date
        } else {
            weekCalendarView.findLastVisibleDay()?.date?.yearMonth?.atDay(1)
        } ?: return@OnCheckedChangeListener

        if (isChecked) {
            weekCalendarView.scrollToWeek(date)
        } else {
            monthCalendarView.scrollToMonth(date.yearMonth)
        }

        val oldHeight = if (isChecked) monthCalendarView.height else weekCalendarView.height
        val newHeight = if (isChecked) weekCalendarView.height else monthCalendarView.height

        ValueAnimator.ofInt(oldHeight, newHeight).apply {
            duration = 250
            addUpdateListener {
                monthCalendarView.updateLayoutParams { height = it.animatedValue as Int }
                monthCalendarView.children.forEach { child -> child.requestLayout() }
            }
            doOnStart {
                if (!isChecked) {
                    weekCalendarView.isInvisible = true
                    monthCalendarView.isVisible = true
                }
            }
            doOnEnd {
                if (isChecked) {
                    weekCalendarView.isVisible = true
                    monthCalendarView.isInvisible = true
                } else {
                    monthCalendarView.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
                }
                updateTitle()
            }
            start()
        }
    }

    private fun applyInsets(binding: Example1FragmentBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.exOneAppBarLayout.updatePadding(top = systemInsets.top)
            binding.root.updatePadding(
                left = systemInsets.left,
                right = systemInsets.right,
                bottom = systemInsets.bottom
            )
            insets
        }
    }
}
