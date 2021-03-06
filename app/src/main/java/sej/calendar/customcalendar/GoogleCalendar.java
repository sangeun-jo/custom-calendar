package sej.calendar.customcalendar;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import sej.calendar.customcalendar.model.Memo;

/**
 * Created by Administrator on 2017-05-26.
 */

public class GoogleCalendar {

    private com.google.api.services.calendar.Calendar mService = null;

    SimpleDateFormat yMd = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
    SimpleDateFormat yMMdd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    SimpleDateFormat ymdhms = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX", Locale.getDefault());

    Realm realm;

    private GoogleCalendar(com.google.api.services.calendar.Calendar mService) {
        this.mService = mService;
    }

    public static GoogleCalendar build(GoogleAccountCredential mCredential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        com.google.api.services.calendar.Calendar mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Google Calendar API Android Quickstart")
                .build();

        return new GoogleCalendar(mService);
    }

    public String getCalendarID(String calendarTitle) throws IOException, UserRecoverableAuthException {
        String id = null;
        String pageToken = null;
        do {
            CalendarList calendarList = null;
            calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            List<CalendarListEntry> items = calendarList.getItems();
            for (CalendarListEntry calendarListEntry : items) {
                if ( calendarListEntry.getSummary().equals(calendarTitle)) {
                    id = calendarListEntry.getId();
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return id;
    }

    public ArrayList<Memo> getEventByDate(String calendarId)  throws IOException, ParseException{
        ArrayList<Memo> eventList = new ArrayList<>();
        String pageToken = null;
        do {
            Events events = mService.events()
                    .list(calendarId)
                    .setPageToken(pageToken)
                    //.setTimeMin()
                    //.setTimeMax()
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                String date;
                if(event.getStart().getDate() != null) {
                    date = yMd.format(yMMdd.parse(event.getStart().getDate().toString()).getTime());
                } else {
                    date = yMd.format(ymdhms.parse(event.getStart().getDateTime().toString()).getTime());
                }
                Memo memo = new Memo();
                memo.setDate(date);
                memo.setTitle(event.getSummary());
                memo.setContent(event.getDescription());
                eventList.add(memo);
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        return eventList;
    }


    //이벤트 리스트 가져오기
    public HashMap<String,Memo> getEventByDate(String calendarId, DateTime start, DateTime end)  throws IOException, ParseException{
        HashMap<String, Memo> memoList = new HashMap<>();
        String pageToken = null;
        do {
            Events events = mService.events()
                    .list(calendarId)
                    .setPageToken(pageToken)
                    .setTimeMin(start)
                    .setTimeMax(end)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();


            for (Event event : items) {
                String date;

                //일단 당일 데이터만...
                Date startDate;
                Date endDate;

                if (event.getStart().getDateTime() != null) {
                    startDate = yMMdd.parse(event.getStart().getDateTime().toString());
                    endDate = yMMdd.parse(event.getEnd().getDateTime().toString());
                } else {
                    startDate = yMMdd.parse(event.getStart().getDate().toString());
                    endDate = yMMdd.parse(event.getStart().getDate().toString());
                }

                if(startDate.equals(endDate)) { //시작시간 == 종료시간
                    date = yMd.format(startDate.getTime());
                    Memo memo = new Memo();
                    memo.setDate(date);
                    memo.setTitle(event.getSummary());
                    memo.setContent(event.getDescription());
                    memoList.put(date, memo);
                }
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);
        return memoList;
    }


    public List<String> getCalendarList() throws IOException, UserRecoverableAuthIOException{
        List<String> result = new ArrayList<>();
        String pageToken = null;
        do {
            CalendarList calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            List<CalendarListEntry> items = calendarList.getItems();
            for(int i =0; i < items.size(); i++) {
                result.add(items.get(i).getSummary()) ;
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);
        return result;
    }

    public void createEvents(ArrayList<Memo> savedMemo, String calendarId) {

        for (Memo m: savedMemo) {
            Event event = new Event()
                    .setSummary(m.getTitle())
                    .setDescription(m.getContent());

            DateTime startDateTime = null;
            try {
                startDateTime = new DateTime(yMd.parse(m.getDate()).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            EventDateTime start = new EventDateTime()
                    .setDate(startDateTime);
            event.setStart(start);
            event.setEnd(start);

            /*리마인더 설정부분
            String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
            event.setRecurrence(Arrays.asList(recurrence));

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail("lpage@example.com"),
                    new EventAttendee().setEmail("sbrin@example.com"),
            };
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(10),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

             */

            try {
                event = mService.events().insert(calendarId, event).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.printf("Event created: %s\n", event.getHtmlLink());
        }
    }


    private String createCalendar() throws IOException, UserRecoverableAuthException {
        String ids = getCalendarID("Custom Calendar");
        if ( ids != null ){
            return "Calendar already exist";
        }
        // 새로운 캘린더 생성
        com.google.api.services.calendar.model.Calendar calendar = new Calendar();
        // 캘린더의 제목 설정
        calendar.setSummary("Custom Calendar");
        // 캘린더의 시간대 설정
        //calendar.setTimeZone("Asia/Seoul");
        // 구글 캘린더에 새로 만든 캘린더를 추가
        Calendar createdCalendar = mService.calendars().insert(calendar).execute();
        // 추가한 캘린더의 ID를 가져옴.
        String calendarId = createdCalendar.getId();
        // 구글 캘린더의 캘린더 목록에서 새로 만든 캘린더를 검색
        CalendarListEntry calendarListEntry = mService.calendarList().get(calendarId).execute();
        // 캘린더의 배경색을 파란색으로 표시  RGB
        calendarListEntry.setBackgroundColor("#0000ff");
        // 변경한 내용을 구글 캘린더에 반영
        CalendarListEntry updatedCalendarListEntry =
                mService.calendarList()
                        .update(calendarListEntry.getId(), calendarListEntry)
                        .setColorRgbFormat(true)
                        .execute();
        // 새로 추가한 캘린더의 ID를 리턴
        return "Custom Calendar Created";
    }



}