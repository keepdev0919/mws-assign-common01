package com.example.photoviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 메인 화면: 동기화 버튼으로 서버에서 이미지 목록을 내려받아 표시.
 */
public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private final List<Bitmap> bitmaps = new ArrayList<>();
    private static final String API = "http://10.0.2.2:8000/api_root/Post/?format=json";
    private static final String API_UPLOAD = "http://10.0.2.2:8000/api_root/Post/";

    private static final int REQ_PICK_IMAGE = 1001;
    private String pendingTitle;
    private String pendingText;
    private android.net.Uri pendingImageUri;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabTop;
    private android.widget.TextView tvLastUpdated;
    private boolean isGrid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        fabTop = findViewById(R.id.fabTop);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImageAdapter(bitmaps);
        recyclerView.setAdapter(adapter);

        // 편의 기능 1: 당겨서 새로고침
        swipeRefreshLayout.setOnRefreshListener(() -> new CloadImage().execute());

        Button btnSync = findViewById(R.id.btnSync);
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CloadImage().execute();
            }
        });

        Button btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUploadDialog();
            }
        });

        // 편의 기능 2: 스크롤 시 상단 이동 FAB 표시/숨김
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy > 20) fabTop.setVisibility(View.VISIBLE); else if (dy < -20) fabTop.setVisibility(View.GONE);
            }
        });
        fabTop.setOnClickListener(v -> recyclerView.smoothScrollToPosition(0));
    }

    // 편의 기능 4: 메뉴에서 캐시 비우기(메모리 해제 + 리스트 초기화)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear) {
            bitmaps.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "캐시를 비웠습니다", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_grid) {
            isGrid = !isGrid;
            if (isGrid) {
                recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                Toast.makeText(this, "그리드 보기", Toast.LENGTH_SHORT).show();
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                Toast.makeText(this, "리스트 보기", Toast.LENGTH_SHORT).show();
            }
            recyclerView.setAdapter(adapter);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 서버에서 JSON을 받아 각 image URL의 비트맵을 다운로드.
     */
    private class CloadImage extends AsyncTask<Void, Void, List<Bitmap>> {
        @Override
        protected void onPreExecute() { if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true); }
        @Override
        protected List<Bitmap> doInBackground(Void... voids) {
            List<Bitmap> list = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(API).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.connect();

                InputStream is = conn.getInputStream();
                String json = new String(is.readAllBytes());
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String imageUrl = o.getString("image");
                    HttpURLConnection ic = (HttpURLConnection) new URL(imageUrl).openConnection();
                    ic.connect();
                    Bitmap bmp = BitmapFactory.decodeStream(ic.getInputStream());
                    list.add(bmp);
                    ic.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<Bitmap> result) {
            bitmaps.clear();
            bitmaps.addAll(result);
            adapter.notifyDataSetChanged();
            Toast.makeText(MainActivity.this,
                    "이미지 로드 성공! (" + result.size() + ")",
                    Toast.LENGTH_SHORT).show();
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            // 편의 기능 3: 마지막 동기화 시간 표기
            tvLastUpdated.setText("마지막 동기화: " + java.time.LocalTime.now().withNano(0).toString());
        }
    }

    /**
     * 업로드 UI: 제목/텍스트 입력 → 갤러리에서 이미지 선택 → 업로드 실행
     */
    private void showUploadDialog() {
        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("제목");
        final EditText inputText = new EditText(this);
        inputText.setHint("본문");

        LinearLayoutCompat layout = new LinearLayoutCompat(this);
        layout.setOrientation(LinearLayoutCompat.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(p, p, p, p);
        layout.addView(inputTitle);
        layout.addView(inputText);

        new AlertDialog.Builder(this)
                .setTitle("새로운 이미지 게시")
                .setView(layout)
                .setPositiveButton("이미지 선택", (d, w) -> {
                    pendingTitle = inputTitle.getText().toString();
                    pendingText = inputText.getText().toString();
                    pickImage();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "이미지 선택"), REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            pendingImageUri = data.getData();
            if (pendingImageUri != null) {
                new PutPost(pendingTitle, pendingText, pendingImageUri).execute();
            }
        }
    }

    /**
     * Multipart POST로 이미지+텍스트 업로드
     */
    private class PutPost extends AsyncTask<Void, Void, String> {
        private final String title;
        private final String text;
        private final android.net.Uri imageUri;

        PutPost(String title, String text, android.net.Uri imageUri) {
            this.title = title;
            this.text = text;
            this.imageUri = imageUri;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String boundary = "----PhotoViewerBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_UPLOAD);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoInput(true);

                OutputStream os = conn.getOutputStream();

                // 텍스트 필드
                writeFormField(os, boundary, "title", title);
                writeFormField(os, boundary, "text", text);

                // 파일 필드
                ContentResolver cr = getContentResolver();
                String fileName = "upload.jpg";
                String mime = cr.getType(imageUri);
                if (mime == null) mime = "image/jpeg";
                writeFileFieldHeader(os, boundary, "image", fileName, mime);
                try (InputStream is = cr.openInputStream(imageUri)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        os.write(buf, 0, n);
                    }
                }
                os.write("\r\n".getBytes());

                // 종료 바운더리
                os.write(("--" + boundary + "--\r\n").getBytes());
                os.flush();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    return "OK";
                } else {
                    InputStream es = conn.getErrorStream();
                    if (es != null) {
                        String msg = new String(es.readAllBytes());
                        return "ERR:" + code + " " + msg;
                    }
                    return "ERR:" + code;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "EX:" + e.getClass().getSimpleName() + ": " + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if ("OK".equals(result)) {
                Toast.makeText(MainActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();
                new CloadImage().execute();
            } else {
                Toast.makeText(MainActivity.this, "업로드 실패: " + result, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void writeFormField(OutputStream os, String boundary, String name, String value) throws Exception {
        String part = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" +
                (value == null ? "" : value) + "\r\n";
        os.write(part.getBytes());
    }

    private void writeFileFieldHeader(OutputStream os, String boundary, String name, String fileName, String mime) throws Exception {
        String part = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: " + mime + "\r\n\r\n";
        os.write(part.getBytes());
    }
}


