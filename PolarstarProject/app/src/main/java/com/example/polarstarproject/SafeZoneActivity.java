package com.example.polarstarproject;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.SafeZone;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Timer;
import java.util.TimerTask;

public class SafeZoneActivity extends AppCompatActivity {
    private ArrayList<SafeZone> mArrayList;
    private SafeZoneRecyclerViewAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    private WarningDialog deleteDialog; //보호구역 삭제 다이얼로그 팝업
    private AuthorityDialog safeZoneDialog; //권한 다이얼로그 팝업
    private DisconnectDialog disconnectDialog; //연결끊기 다이얼로그 팝업

    private static final String TAG = "SafeZone";

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = database.getReference();

    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수

    private int count = -1;
    Toolbar toolbar;
    Button btn_Set;

    Timer timer; //상대방과 매칭 검사를 위한 타이머
    TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safezone_setting);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("보호구역 관리");


        btn_Set = findViewById(R.id.btn_Set);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_main_list);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        //다이얼로그 초기 설정
        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        //다이얼로그 초기 설정
        safeZoneDialog = new AuthorityDialog(this, null);
        safeZoneDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        skipScreen();

        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mArrayList = new ArrayList<>();
        mAdapter = new SafeZoneRecyclerViewAdapter(mArrayList, this);
        mRecyclerView.setAdapter(mAdapter);

        databaseReference.child("safezone").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 파이어베이스 데이터베이스의 데이터를 받아오는 곳
                mArrayList.clear(); // 기존 배열리스트가 존재하지않게 초기화
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // 반복문으로 데이터 List를 추출해냄
                    SafeZone sList = snapshot.getValue(SafeZone.class); // 만들어뒀던 User 객체에 데이터를 담는다.
                    mArrayList.add(sList); // 담은 데이터들을 배열리스트에 넣고 리사이클러뷰로 보낼 준비
                }
                mAdapter.notifyDataSetChanged(); // 리스트 저장 및 새로고침해야 반영이 됨
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 디비를 가져오던중 에러 발생 시
                Log.e("Fraglike", String.valueOf(databaseError.toException())); // 에러문 출력
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                mLinearLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        //이버튼이 눌리면
        btn_Set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mEmailText.setText(setId); // 자동로그인 해놨을 때만 가능
                if(mAdapter.getItemCount()<5){
                    Intent intent = new Intent(getApplicationContext(), RangeSettingActivity.class);
                    startActivity(intent);
                } else {
                    safeZoneDialog = new AuthorityDialog(SafeZoneActivity.this, "보호구역은 5개까지만 설정할 수 있습니다.");
                    safeZoneDialog.setCancelable(false);
                    safeZoneDialog.show();
                    safeZoneDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
                }

            }
        });

        //리사이클러뷰 클릭 이벤트
        mAdapter.setOnItemClickListener (new SafeZoneRecyclerViewAdapter.OnItemClickListener() {
            /*
            @Override
            public void onEditClick(View v, int position) {
                String name = mArrayList.get (position).getName ();

            }

             */

            //삭제
            @Override
            public void onDeleteClick(View v, int position) {
                deleteDialog = new WarningDialog(SafeZoneActivity.this, "보호구역을 삭제하시겠습니까?");
                deleteDialog.show(); // 다이얼로그 띄우기
                deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게

                //취소 버튼
                Button btnCancle = deleteDialog.findViewById(R.id.btn_cancle);
                btnCancle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 원하는 기능 구현
                        deleteDialog.dismiss(); // 다이얼로그 닫기
                    }
                });

                //확인 버튼
                Button btnOk = deleteDialog.findViewById(R.id.btn_ok);
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 원하는 기능 구현
                        deleteDialog.dismiss(); // 다이얼로그 닫기
                        Toast.makeText(getApplicationContext(),"보호구역이 삭제되었습니다." , Toast.LENGTH_SHORT).show();
                        String name = mArrayList.get(position).getName();
                        ItemDelete(name);
                        mArrayList.remove (position);
                        mAdapter.notifyItemRemoved (position);
                    }
                });
            }

            //뷰 아이템 클릭 이벤트
            @Override
            public void onItemClick(View v, int position) {
                String name = mArrayList.get(position).getName();
                String address = mArrayList.get(position).getAddress();
                int dis = mArrayList.get(position).getDis();

                Intent intent = new Intent(getApplicationContext(), SafeZoneConfirmActivity.class);
                intent.putExtra("rName",name); // 보호구역이름 넘기기
                intent.putExtra("rAddress",address);
                intent.putExtra("rDis",dis);

                startActivity(intent);
                finish(); //화면 이동
            }

        });

    }
    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
                startActivity(intent);
                finish(); //화면 이동
                timer.cancel();
                timerTask.cancel(); //타이머 종료

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
        startActivity(intent);
        finish(); //화면 이동
        timer.cancel();
        timerTask.cancel(); //타이머 종료
    }

    /////////////////////////////////////////연결 체크////////////////////////////////////////
    private void startDisconnectDialog(){
        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.setCancelable(false);
        disconnectDialog.show();
        disconnectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
    }

    /////////////////////////////////////////연결 여부 확인 후 화면 넘어가기////////////////////////////////////////
    private void skipScreen(){
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                //1초마다 실행
                connectionCheck(); //상대방과 매칭 여부 확인
                Log.w(TAG, "돌아감");
            }
        };
        timer.schedule(timerTask,0,1000);
    }

    /////////////////////////////////////////연결 여부 확인////////////////////////////////////////
    private void connectionCheck(){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query guardianQuery = databaseReference.child("connect").child("guardian").orderByKey().equalTo(user.getUid()); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Connect myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    if(myConnect.getCounterpartyCode() == null){ //상대방이 연결 끊었을 경우
                        if(! SafeZoneActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                            timer.cancel();
                            timerTask.cancel(); //타이머 종료
                        }
                        Log.w(TAG, "상대 피보호자 없음");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void ItemDelete(String name){
        databaseReference.child("safezone").child(user.getUid()).child(name).setValue(null);
        databaseReference.child("range").child(user.getUid()).child(name).setValue(null);
    }

}