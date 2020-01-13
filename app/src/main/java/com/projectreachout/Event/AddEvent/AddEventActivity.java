package com.projectreachout.Event.AddEvent;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.projectreachout.AppController;
import com.projectreachout.Event.AddEvent.BottomSheets.BottomSheetFragment;
import com.projectreachout.R;
import com.projectreachout.SelectPeople.SelectPeopleActivity;
import com.projectreachout.User.User;
import com.projectreachout.Utilities.DateAndTimePicker.DatePickerFragment;
import com.projectreachout.Utilities.DateAndTimePicker.ResponseDate;
import com.projectreachout.Utilities.NetworkUtils.HttpVolleyRequest;
import com.projectreachout.Utilities.NetworkUtils.OnHttpResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.projectreachout.GeneralStatic.DATE_RESPONSE;
import static com.projectreachout.GeneralStatic.EXISTING_ORGANIZERS;
import static com.projectreachout.GeneralStatic.GET_ORGANIZER_LIST;
import static com.projectreachout.GeneralStatic.OPTION;
import static com.projectreachout.GeneralStatic.OPTION_ORGANIZERS;
import static com.projectreachout.GeneralStatic.OPTION_TEAM;
import static com.projectreachout.GeneralStatic.ORGANIZER_LIST;
import static com.projectreachout.GeneralStatic.SELECTED_ORGANIZERS;
import static com.projectreachout.GeneralStatic.SPARSE_BOOLEAN_ARRAY;
import static com.projectreachout.GeneralStatic.TEAM_LIST;
import static com.projectreachout.GeneralStatic.getDummyUrl;
import static com.projectreachout.GeneralStatic.getMonthForInt;
import static com.projectreachout.GeneralStatic.showKeyBoard;

public class AddEventActivity extends AppCompatActivity implements OnHttpResponse {

    private static final String TAG = AddEventActivity.class.getSimpleName();

    private TextView mShowDateTV;
    private TextView mShowTeamTV;
    private TextView mShowOrganizersTV;
    private TextView mShowEventLeaderTV;

    private Button mPickDateBtn;
    private Button mPickTeamBtn;
    private Button mSelectPeopleBtn;
    private Button mSelectEventLeaderBtn;

    private EditText mTitleET;
    private EditText mDescriptionET;

    private Button mSubmitBtn;

    private String mDate;
    private String mEventLeader;
    private String mEventLeaderId;
    private int mEventLeaderIndex = -1;
    private ArrayList<String> mSelectedTeam = new ArrayList<>();
    private ArrayList<User> mSelectedUsers = new ArrayList<>();

    private ProgressDialog mDialog;

    // TODO: Delete this SparseBooleanArray after item selection works with base on user id
    private SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ane_activity_add_event);

        try {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(getString(R.string.title_add_event));
            actionBar.setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        mShowDateTV = findViewById(R.id.tv_aaae_show_date);
        mShowTeamTV = findViewById(R.id.tv_aaae_show_team);
        mShowOrganizersTV = findViewById(R.id.tv_aaae_show_selected_people);
        mShowEventLeaderTV = findViewById(R.id.tv_aaae_show_event_leader);

        // Underlining text for clickable
        mShowTeamTV.setPaintFlags(mShowTeamTV.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mShowOrganizersTV.setPaintFlags(mShowOrganizersTV.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        mPickDateBtn = findViewById(R.id.btn_aaae_pick_date);
        mPickTeamBtn = findViewById(R.id.btn_aaae_pick_team);
        mSelectPeopleBtn = findViewById(R.id.btn_aaae_select_people);
        mSelectEventLeaderBtn = findViewById(R.id.btn_aaae_select_event_leader);

        mTitleET = findViewById(R.id.et_aaae_title);
        mDescriptionET = findViewById(R.id.et_aaae_description);

        mSubmitBtn = findViewById(R.id.btn_aaae_submit);

        mDialog = new ProgressDialog(this);

        displayDateToday();

        mShowTeamTV.setOnClickListener(this::showSelectedTeams);
        mShowOrganizersTV.setOnClickListener(this::showSelectedOrganizers);

        mPickDateBtn.setOnClickListener(this::pickEventDate);
        mPickTeamBtn.setOnClickListener(this::showTeamPicker);
        mSelectPeopleBtn.setOnClickListener(this::navigateSelectPeopleActivity);
        mSelectEventLeaderBtn.setOnClickListener(this::selectEventLeader);

        mSubmitBtn.setOnClickListener(this::submitEvent);
    }

    String eventLeader = null;
    String eventLeaderId = null;
    int eventLeaderIndex = -1;

    private void selectEventLeader(View view) {
        if (mSelectedUsers.size() == 0) {
            String errorMessage = "Select Organizers First";
            Snackbar.make(view, errorMessage, Snackbar.LENGTH_INDEFINITE).setAction("Select", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateSelectPeopleActivity(new View(getApplicationContext()));
                }

            }).show();
            return;
        }

        String title = "Choose Event Leader";
        String[] teamName = new String[mSelectedUsers.size()];

        for (int i=0; i<mSelectedUsers.size(); i++) {
            User user = mSelectedUsers.get(i);
            teamName[i] = user.getDisplay_name();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title);
        builder.setSingleChoiceItems(teamName, mEventLeaderIndex, (dialog, which) -> {
            eventLeader = teamName[which];
            eventLeaderId = mSelectedUsers.get(which).getUser_id();
            Log.v(TAG, "eventLeader: " + eventLeader + " : " + mSelectedUsers.get(which).getDisplay_name());
            eventLeaderIndex = which;
        }).setPositiveButton("OK", (dialog, which) -> {
            mEventLeader = eventLeader;
            mEventLeaderId = eventLeaderId;
            mEventLeaderIndex = eventLeaderIndex;
            mShowEventLeaderTV.setText(mEventLeader);
        }).setNegativeButton("Cancel", (dialog, which) -> {

        });

        builder.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateSelectPeopleActivity(View view) {
        // TODO: Delete this SparseBooleanArray part after item selection works with base on user id
        ArrayList<Integer> integerArrayList = new ArrayList<>();
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            integerArrayList.add(sparseBooleanArray.keyAt(i));
        }

        Intent intent = new Intent(view.getContext(), SelectPeopleActivity.class);
        intent.putParcelableArrayListExtra(EXISTING_ORGANIZERS, mSelectedUsers);
        intent.putIntegerArrayListExtra(SPARSE_BOOLEAN_ARRAY, integerArrayList);

        startActivityForResult(intent, GET_ORGANIZER_LIST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_ORGANIZER_LIST) {
                mSelectedUsers = new ArrayList<>();
                mSelectedUsers.addAll(data != null ? data.getParcelableArrayListExtra(SELECTED_ORGANIZERS) : new ArrayList<>());

                // TODO: Delete this SparseBooleanArray part after item selection works with base on user id
                ArrayList<Integer> integerArrayList = data != null ? data.getIntegerArrayListExtra(SPARSE_BOOLEAN_ARRAY) : new ArrayList<>();
                sparseBooleanArray = new SparseBooleanArray();
                for (int i = 0; i < integerArrayList.size(); i++) {
                    sparseBooleanArray.put(integerArrayList.get(i), true);
                }
                Log.v(TAG, mSelectedUsers.toString());

                if (mSelectedUsers.size() == 0) {
                    mShowOrganizersTV.setText("None");
                } else {
                    mShowOrganizersTV.setText("Organizers: " + mSelectedUsers.size());
                }

                boolean flag = false;
                for (int i=0; i<mSelectedUsers.size(); i++) {
                    if (mSelectedUsers.get(i).getDisplay_name().equals(mEventLeader)) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    mShowEventLeaderTV.setText("None");
                    mEventLeader = null;
                    mEventLeaderIndex = -1;
                }

                Log.d(TAG, mSelectedUsers.toString());
            }
        }
    }

    private void showSelectedOrganizers(View view) {
        Bundle bundle = new Bundle();
        bundle.putInt(OPTION, OPTION_ORGANIZERS);
        bundle.putParcelableArrayList(ORGANIZER_LIST, mSelectedUsers);

        BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();
        bottomSheetFragment.setArguments(bundle);

        bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    private void showSelectedTeams(View view) {
        Bundle bundle = new Bundle();
        bundle.putInt(OPTION, OPTION_TEAM);
        bundle.putStringArrayList(TEAM_LIST, mSelectedTeam);

        BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();
        bottomSheetFragment.setArguments(bundle);

        bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    private void pickEventDate(View view) {
        DialogFragment newFragment = new DatePickerFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(DATE_RESPONSE, new ResponseDate() {
            @Override
            public void setDate(int year, int month, int day) {
                int realMonth = month + 1;
                mDate = year + "-" + realMonth + "-" + day;
                mShowDateTV.setText(day + " " + getMonthForInt(month) + " " + year);
                Log.v(TAG, mDate);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {

            }
        });
        newFragment.setArguments(bundle);
        newFragment.show(getSupportFragmentManager(), "date_picker");
    }

    private Map<String, String> getMappedData() {

        // TODO: Improve to Handle Null Exceptions, errors and empty EditTexts
        String title = mTitleET.getText().toString().trim();
        String description = mDescriptionET.getText().toString().trim();

        // TODO: Implement getDisplay_name() and replace with the current user
        String username = AppController.getInstance().getFirebaseAuth().getUid();

        List<String> organizersId = new ArrayList<>();
        for (int i = 0; i < mSelectedUsers.size(); i++) {
            // organizersId.add(mSelectedUsers.get(i).getUser_id());
            organizersId.add(mSelectedUsers.get(i).getUser_id());
        }

        Map<String, String> param = new HashMap<>();
        param.put("assigned_by", username);
        param.put("event_date", mDate);
        param.put("event_title", title);
        param.put("description", description);
        param.put("selected_teams", mSelectedTeam.toString());
        param.put("organizers", organizersId.toString());
        param.put("event_leader", mEventLeaderId);

        Log.v(TAG, "MAP: \n\n\n" + param.toString() + "\n\n\n");
        return param;
    }

    private void submitEvent(View view) {

        if (mSelectedTeam.size() == 0) {
            showTeamPicker(new View(getApplicationContext()));
            return;
        }

        if (mSelectedUsers.size() == 0) {
            navigateSelectPeopleActivity(new View(getApplicationContext()));
            return;
        }

        if (mEventLeader == null || mEventLeader.trim().equals("")) {
            selectEventLeader(new View(getApplicationContext()));
            return;
        }

        String description = mTitleET.getText().toString().trim();
        if (description.equals("")) {
            showKeyBoard(mTitleET);
            return;
        }

        Map<String, String> param = getMappedData();
        String url = getDummyUrl() + "/add_event/";

        Log.v(TAG, param.toString());

        mDialog.setMessage("Loading. Please wait...   ");
        mDialog.show();

        HttpVolleyRequest httpVolleyRequest = new HttpVolleyRequest(Request.Method.POST, url, null, 0, null, param, this);
        httpVolleyRequest.execute();
    }

    private void displayErrorMessage() {
        String errorMessage = "Couldn't update information to server...";
        View parentView = findViewById(android.R.id.content);
        Snackbar.make(parentView, errorMessage, Snackbar.LENGTH_INDEFINITE).setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitEvent(v);
            }
        }).show();
    }

    ArrayList<String> selectedTeam = new ArrayList<>();

    private void showTeamPicker(View view) {
        String title = "Choose Team";
        String[] teamName = new String[]{"Regular Volunteers", "Fund Raising", "Event", "Environmental Awareness"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        boolean[] checkedItem = new boolean[teamName.length];
        if (mSelectedTeam.size() != 0) {
            selectedTeam = new ArrayList<>();
            for (int i = 0; i < mSelectedTeam.size(); i++) {
                for (int j = 0; j < teamName.length; j++) {
                    if (mSelectedTeam.get(i).equals(teamName[j])) {
                        selectedTeam.add(teamName[j]);
                        checkedItem[j] = true;
                        break;
                    }
                }
            }
        }

        builder.setTitle(title).setMultiChoiceItems(teamName, checkedItem, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedTeam.add(teamName[which]);
            } else {
                selectedTeam.remove(teamName[which]);
            }
        }).setPositiveButton("OK", (dialog, which) -> {
            /*StringBuilder s = new StringBuilder();
            for (int i = 0; i < selectedTeam.size(); i++) {
                s.append(selectedTeam.get(i));
            }*/
            mSelectedTeam = new ArrayList<>();
            mSelectedTeam = selectedTeam;

            if (mSelectedTeam.size() == 0) {
                mShowTeamTV.setText("None");
            } else {
                mShowTeamTV.setText("Team: " + mSelectedTeam.size());
            }
        }).setNegativeButton("Cancel", (dialog, which) -> {

        });

        builder.create().show();
    }

    private void displayDateToday() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;      // month starts from 0
        int day = c.get(Calendar.DAY_OF_MONTH);

        mDate = year + "-" + month + "-" + day;
        mShowDateTV.setText(day + " " + getMonthForInt(month-1) + " " + year);
    }

    @Override
    public void onHttpResponse(String response, int request) {
        Log.v(TAG, response);
        if (request == 0) {
            if (response.trim().equals("200")) {
                mDialog.dismiss();
                Toast.makeText(AddEventActivity.this, "Event Added", Toast.LENGTH_SHORT).show();
                onBackPressed();
            } else {
                mDialog.dismiss();
                displayErrorMessage();
            }
        }
    }

    @Override
    public void onHttpErrorResponse(VolleyError error, int request) {
        Log.v(TAG, error.toString());
        if (request == 0) {
            mDialog.dismiss();
            displayErrorMessage();
        }
    }
}