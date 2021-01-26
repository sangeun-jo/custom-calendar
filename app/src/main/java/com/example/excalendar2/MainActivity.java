package com.example.excalendar2;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;;
import java.util.Locale;


// 12월 달력을 기준으로 그리고, 밑에 조그맣게 정혈 주기 별 커스텀 달력으로 보이게 해야겠다.
// 옵션으로 정혈달력을 기준으로 사용할 수 있음
// 하단 바 있는 프레그먼트 레이아웃으로 만들기
// 메뉴-오늘, 정혈 달력, 통계, 설정
// 요일 레이블 추가, 시작 요일 선택에 따라 바뀌게 하기

public class MainActivity extends AppCompatActivity {

    public int curYear; //13월 달력 기준 년
    public int curMonth; //13월 달력 기준 월
    public GridView gridView;
    public TextView tvCalendarTitle;
    private TextView tvSelectedDate;

    private ArrayList<DayInfo> arrayListDayInfo = new ArrayList<>();

    CalendarAdapter calendarAdapter; //캘린더 어댑터 객체
    CustomCalendar cCal = new CustomCalendar();
    String selectedDate; // 선택된 날짜. 13월 기준

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        tvCalendarTitle = findViewById(R.id.tv_calendar_title); //제목
        tvSelectedDate = findViewById(R.id.iv_selected);
        gridView = findViewById(R.id.gv_calendar);  //그리드 뷰
        gridView.setNumColumns(cCal.DAY_PER_WEEK);

        ImageButton btnPreviousCalendar = findViewById(R.id.btn_previous_calendar); //이전달 버튼
        ImageButton btnNextCalendar = findViewById(R.id.btn_next_calendar); // 다음달 버튼
        ImageButton btnPreviousCalendar2 = findViewById(R.id.btn_previous_calendar2); //이전년 버튼
        ImageButton btnNextCalendar2 = findViewById(R.id.btn_next_calendar2); // 다음년 버튼
        //ImageButton btnToday = findViewById(R.id.btn_today);

        Calendar cal = Calendar.getInstance(Locale.KOREA);  //달력 객체
        selectedDate = cCal.nToC(cal); //현재 날짜로 변환

        String[] Today = selectedDate.split("-");
        curYear = Integer.parseInt(Today[0]);
        curMonth = Integer.parseInt(Today[1]);

        drawCalendar(curYear, curMonth);  //오늘 날짜

        //월 이동 버튼
        btnPreviousCalendar.setOnClickListener(new View.OnClickListener(){ // 이전달 버튼이 눌리면
            @Override
            public void onClick(View view) {
                if(curMonth <= 1){
                    curYear -= 1; curMonth = cCal.MONTH_PER_YEAR;
                } else { curMonth -= 1; }
                changeYM(curYear, curMonth);
                drawCalendar(curYear, curMonth);
            }
        });

        btnNextCalendar.setOnClickListener(new View.OnClickListener(){ //다음달 버튼 눌리면
            @Override
            public void onClick(View view) {
                if(curMonth == cCal.MONTH_PER_YEAR){
                    curYear += 1; curMonth = 1;
                } else { curMonth += 1; }
                changeYM(curYear, curMonth);
                drawCalendar(curYear, curMonth);
            }
        });

        //년 이동 버튼
        btnPreviousCalendar2.setOnClickListener(new View.OnClickListener(){ // 이전달 버튼이 눌리면
            @Override
            public void onClick(View view) {
                curYear -= 1;
                drawCalendar(curYear, curMonth);
            }
        });
        btnNextCalendar2.setOnClickListener(new View.OnClickListener(){ //다음달 버튼 눌리면
            @Override
            public void onClick(View view) {
                curYear += 1;
                drawCalendar(curYear, curMonth);
            }
        });

        // 날짜 쉘 클릭 시
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // 날짜 중에 아무거나 선택하면
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DayInfo clickedDay = (DayInfo)view.getTag();
                if(clickedDay.inMonth){
                    setSelectedDate(clickedDay.getDate());
                } else{
                    return;
                }
                calendarAdapter.notifyDataSetChanged();
            }
        });

    }


    //액션바 옵션 메뉴
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //오늘 날짜 클릭
        switch(item.getItemId()) {
            case R.id.select_date:
                Calendar calendar = Calendar.getInstance();
                curYear = calendar.get(Calendar.YEAR);
                curMonth = calendar.get(Calendar.MONTH) + 1;
                String refresh = curYear + "-" + curMonth + "-" + calendar.get(Calendar.DATE);
                setSelectedDate(refresh);
                drawCalendar(curYear, curMonth);  //오늘 날짜
                calendarAdapter.notifyDataSetChanged();
                break;
            case R.id.setting:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void setSelectedDate(String date){
        selectedDate = date;
        if(calendarAdapter != null){
            calendarAdapter.selectedDate = date;
        }
    }

    public void changeYM(int year, int month){
        tvCalendarTitle.setText(year + "년 " + month + "월");
    }




    //13월 기준으로 13. 12 캘린더 그리는 함수
    public void drawCalendar(int year, int month) {
        arrayListDayInfo.clear();

        Calendar jen = Calendar.getInstance(Locale.KOREA);
        jen.set(year, 0, 1);
        int dayOfWeek = jen.get(Calendar.DAY_OF_WEEK);

        DayInfo day;

        // 달력 앞 빈 칸
        if (dayOfWeek == 1) {
            for (int i = 0; i < cCal.DAY_PER_WEEK - (cCal.FIRST_WEEK-1); i++) {
                day = new DayInfo();
                arrayListDayInfo.add(day);
            }
        } else { //월화수목금토
            for (int i = 0; i < dayOfWeek-cCal.FIRST_WEEK; i++) { //7-2
                day = new DayInfo();
                arrayListDayInfo.add(day);
            }
        }

        // 숫자 쉘
        if (month != cCal.MONTH_PER_YEAR) {
            for (int i = 1; i <= cCal.DAY_PER_MONTH; i++) {
                day = new DayInfo();
                day.setDate(year, month ,i,true);
                arrayListDayInfo.add(day);
            }
        } else { // 마지막 달
            for (int i = 1; i <= cCal.LAST_MONTH_DAY; i++) {
                day = new DayInfo();
                day.setDate(year, month ,i,true);
                arrayListDayInfo.add(day);
            }
        }

        // 달력 뒷부분 빈칸
        int lastWeek = cCal.DAY_PER_WEEK - arrayListDayInfo.size() % cCal.DAY_PER_WEEK;
        for(int i=0; i<lastWeek; i++) {
            day = new DayInfo();
            arrayListDayInfo.add(day);
        }

        // 연, 월 그리기
        tvCalendarTitle.setText(year + "년 " + month + "월");

        //그리드 뷰에 그리기
        calendarAdapter = new CalendarAdapter(selectedDate, arrayListDayInfo);
        gridView.setAdapter(calendarAdapter);
    }
}