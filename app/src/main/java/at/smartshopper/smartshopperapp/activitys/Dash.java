package at.smartshopper.smartshopperapp.activitys;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import at.smartshopper.smartshopperapp.R;
import at.smartshopper.smartshopperapp.db.Database;
import at.smartshopper.smartshopperapp.messaging.MyFirebaseSender;
import at.smartshopper.smartshopperapp.shoppinglist.Shoppinglist;
import at.smartshopper.smartshopperapp.shoppinglist.ShoppinglistAdapter;
import at.smartshopper.smartshopperapp.shoppinglist.ShoppinglistSharedAdapter;


public class Dash extends AppCompatActivity implements ShoppinglistAdapter.OnItemClicked, ShoppinglistAdapter.OnShoppinglistClick, ShoppinglistAdapter.OnChangeItemClick, ShoppinglistAdapter.OnShareClick, ShoppinglistSharedAdapter.SharedOnItemClicked, ShoppinglistSharedAdapter.SharedOnChangeItemClick, ShoppinglistSharedAdapter.SharedOnShareClick, ShoppinglistSharedAdapter.SharedOnShoppinglistClick {

    private final Database db = new Database();
    private SwipeRefreshLayout ownswiperefresh, sharedswiperefresh;
    private FloatingActionButton addShoppinglistFab;
    private PopupWindow popupWindowAdd, popupShare, popupAddShare, popupEditShare;
    private String color;
    private Button colorBtn;
    //Für Double Back press to exit
    private boolean doubleBackToExitPressedOnce = false;
    private TabHost host;

    /**
     * Convertiert eine int farbe in eine hexa dezimale Farbe
     *
     * @param color Farbe zum umwandeln in int
     * @return farbe als hex im string
     */
    private static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFFFF & color);
    }

    /**
     * Setzt das atribut color wenn die activity colorpicker beendet wird
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                int color = Integer.parseInt(data.getData().toString());
                this.color = colorToHexString(color);
                int colorint = Color.parseColor(this.color);
                colorBtn.setBackgroundTintList(ColorStateList.valueOf(colorint));
                if(ColorUtils.calculateLuminance(colorint)>0.5){
                    colorBtn.setTextColor(Color.parseColor("#000000")); // It's a light color
                }else{
                    colorBtn.setTextColor(Color.parseColor("#FFFFFF")); // It's a dark color
                }
            }
        }
    }

    /**
     * Holt den msg token
     * <p>
     * SETZT IHN NOCH NED
     * <p>
     * <p>
     * WEITER PROGRAMMIERN
     * <p>
     * MIR FEHLT NOCH DIE DB VON LUKAS
     */
    private void setMsgId() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("SmartShopper", "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        Log.d("SmartShopper MSG", token);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.dashToolbar);
        setSupportActionBar(myToolbar);

        color = "ffffff";

        setMsgId();

        Intent getIntent = getIntent();
        String sl_idToGo = getIntent.getStringExtra("sl_idToGo");
        String inviteToAdd = getIntent.getStringExtra("inviteToAdd");

        if (sl_idToGo != null && inviteToAdd != null) {
            try {
                db.addInviteLinkDynamicLink(inviteToAdd, FirebaseAuth.getInstance().getCurrentUser().getUid());
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
//           Damit werden die Shared Shoppinglists angezeigt
            //Damit wird die hinzugefügte shoppinglist angezeigt
            onShoppinglistClickContainer(sl_idToGo);
        }


        /*
        Get userinformations and show them
         */
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();


            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            final String uid = user.getUid();
            // Erstellt die Tabs
            tabHoster(uid);

            // Get the transferred data from source activity.
            Intent intent = getIntent();
            String message = intent.getStringExtra("tab2");
            String wahr = "true";
            if (message != null) {
                if (message.equals(wahr)) {
                    host.setCurrentTab(1);
                }
            }
            try {
                try {
                    showOwnShoppingList(uid);
                    showSharedShoppingList(uid);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sharedswiperefresh = (SwipeRefreshLayout) findViewById(R.id.sharedSwipe);

            sharedswiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    try {
                        showSharedShoppingList(uid);
                        sharedswiperefresh.setRefreshing(false);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
            ownswiperefresh = (SwipeRefreshLayout) findViewById(R.id.ownSwipe);

            ownswiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    try {
                        refreshOwnShoppinglist(uid);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            });

            addShoppinglistFab = (FloatingActionButton) findViewById(R.id.addShoppinglistFab);

            addShoppinglistFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        showShoppinglistEditView(false, null, "Shoppingliste erstellen", v);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }


            });


        }
    }

    /**
     * Zeigt ein Popup das zum Bearbeiten/Erstellen einer Shoppingliste dient.
     * Wenn eine Shoppingliste bearbeitet werden soll, muss fromDB true sein und sl_id mit einer id gefüllt
     * Wenn erstellt werden soll muss fromDB false sein und sl_id null
     *
     * @param fromDB True wenn daten von der DB kommen sollen, wenn false dann muss die sl_id null sein
     * @param sl_id  Muss nur eine sl_id drinnen sein wenn fromDB true ist
     * @param v      der View auf dem das Popup sein soll
     */
    private void showShoppinglistEditView(final boolean fromDB, String sl_id, String title, View v) throws SQLException, JSONException {
        final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        final String username = FirebaseAuth.getInstance().getCurrentUser().getUid();

        View customView = inflater.inflate(R.layout.add_shoppinglist_dialog, null);

        TextView fensterTitle = (TextView) customView.findViewById(R.id.shoppinglisteAddTitle);
        fensterTitle.setText(title);

        ImageButton addClose = (ImageButton) customView.findViewById(R.id.addClose);
        colorBtn = (Button) customView.findViewById(R.id.addColor);
        final Button addFertig = (Button) customView.findViewById(R.id.addFertig);
        final EditText name = (EditText) customView.findViewById(R.id.addName);
        name.setTextIsSelectable(true);
        final EditText description = (EditText) customView.findViewById(R.id.addDescription);
        description.setTextIsSelectable(true);

        Picasso.get().load(R.drawable.close).into(addClose);

        if (!name.getText().toString().isEmpty()) {
            addFertig.setEnabled(true);
        } else {
            addFertig.setEnabled(false);
        }

        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!name.getText().toString().isEmpty()) {
                    addFertig.setEnabled(true);
                } else {
                    addFertig.setEnabled(false);
                }
            }
        });

        if (fromDB) {
            Shoppinglist dbShoppinglist = db.getShoppinglist(sl_id);
            String colorstring;
            if (dbShoppinglist.getcolor().contains("#")) {
                colorstring = dbShoppinglist.getcolor();
            } else {
                colorstring = "#" + dbShoppinglist.getcolor();
            }
            this.color = colorstring;
            int colorint = Color.parseColor(colorstring);
            colorBtn.setBackgroundTintList(ColorStateList.valueOf(colorint));
            if(ColorUtils.calculateLuminance(colorint)>0.5){
                colorBtn.setTextColor(Color.parseColor("#000000")); // It's a light color
            }else{
                colorBtn.setTextColor(Color.parseColor("#FFFFFF")); // It's a dark color
            }
            name.setText(dbShoppinglist.getname());
            description.setText(dbShoppinglist.getdescription());
        } else {
            color = "ffffff";
        }

        final String sl_idString = sl_id;
        addFertig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pushEndString;
                if (fromDB) {
                    try {
                        db.editShoppinglist(sl_idString, name.getText().toString(), description.getText().toString(), color);
                        color = "ffffff";
                        popupWindowAdd.dismiss();
                        showOwnShoppingList(username);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    pushEndString = " wurde geändert!";
                } else {
                    try {
                        db.addShoppinglist(name.getText().toString(), description.getText().toString(), username, color);
                        color = "ffffff";
                        popupWindowAdd.dismiss();
                        showOwnShoppingList(username);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    pushEndString = " wurde erstellt!";
                }

                try {
                    MyFirebaseSender myFirebaseSender = new MyFirebaseSender(db.getMembers(sl_idString));
                    myFirebaseSender.addMember(db.getAdmin(sl_idString));
                    myFirebaseSender.sendMessage(name.getText().toString() + pushEndString + " Von " + db.getUser(username).getName(), name.getText().toString() + pushEndString);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });

        colorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dash.this, Colorpicker.class);
                startActivityForResult(intent, 1);

            }
        });

        addClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindowAdd.dismiss();
            }
        });

        popupWindowAdd = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= 21) {
            popupWindowAdd.setElevation(5.0f);
        }

        popupWindowAdd.setOutsideTouchable(false);
        popupWindowAdd.setFocusable(true);
        popupWindowAdd.setAnimationStyle(R.style.popup_window_animation_phone);


        popupWindowAdd.showAtLocation(v, Gravity.CENTER, 0, 0);
        popupWindowAdd.update();
    }

    /**
     * Logt den User aus und geht zur Login Activity
     */
    private void logout() {
        finish();
        // Configure sign-in to request the user's ID, email address, and basic
// profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(Dash.this, LoginActivity.class);
                startActivity(intent);
            }
        });

    }

    /**
     * Refreshed die eigene shoppinglist und veranlasst das das refreshen beendet wird
     *
     * @param uid Von dem benutzer von welchem die Shoppinglists angezeigt werden sollen
     */
    private void refreshOwnShoppinglist(String uid) throws SQLException {
        try {
            showOwnShoppingList(uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        refreshOwnShoppinglistFinish();
    }

    /**
     * Stoppt das refreshen der OwnShoppinglist
     */
    private void refreshOwnShoppinglistFinish() {
        // Update the adapter and notify data set changed
        // ...

        // Stop refresh animation
        ownswiperefresh.setRefreshing(false);
    }

    /**
     * Macht eine Datenbankverbindung und holt alle Shoppinglists die mit dem User geteilt werden, diese werden auf dem recycled view angezeigt
     *
     * @param uid Die UserId damit von diesem user die shoppinglisten angezeigt werden
     */
    private void showSharedShoppingList(String uid) throws JSONException, SQLException {
        RecyclerView sharedRecycler = (RecyclerView) findViewById(R.id.sharedrecycler);
        sharedRecycler.setHasFixedSize(true);
        sharedRecycler.setLayoutManager(new LinearLayoutManager(this));
        List<Shoppinglist> sharedListsList = db.getSharedShoppinglists(uid);
        ArrayList<Shoppinglist> sharedListsArrayListTmp = new ArrayList<>();
        List<Shoppinglist> sharedListsListTmp;

        if (sharedListsList.isEmpty()) {
            sharedListsArrayListTmp.add(new Shoppinglist("empty", "Keine Shoppingliste geteilt!", "Um einen Invite Link hinzuzufügen, fügen Sie diesen im Menü ein.", "empty", "#8B0000"));
            sharedListsListTmp = sharedListsArrayListTmp;
        } else {
            sharedListsListTmp = sharedListsList;
//            findViewById(R.id.pfeilnachunten3).setVisibility(View.GONE);
        }
        ShoppinglistSharedAdapter shpAdapter = new ShoppinglistSharedAdapter(Dash.this, sharedListsListTmp, db);
        if (sharedListsList.isEmpty()) {
            shpAdapter.setOnDelClick(new ShoppinglistSharedAdapter.SharedOnItemClicked() {
                @Override
                public void sharedOnItemClick(String sl_id) {

                }
            });
            shpAdapter.setOnChangeClick(new ShoppinglistSharedAdapter.SharedOnChangeItemClick() {
                @Override
                public void sharedOnChangeItemClick(String sl_id, View v) {

                }
            });
            shpAdapter.setOnShareClick(new ShoppinglistSharedAdapter.SharedOnShareClick() {
                @Override
                public void sharedOnShareClick(String sl_id, View v) {

                }
            });
            shpAdapter.setOnShoppinglistClick(new ShoppinglistSharedAdapter.SharedOnShoppinglistClick() {
                @Override
                public void sharedOnShoppinglistClick(String sl_id, View v) {

                }
            });
        } else {
            shpAdapter.setOnDelClick(Dash.this);
            shpAdapter.setOnChangeClick(Dash.this);
            shpAdapter.setOnShareClick(Dash.this);
            shpAdapter.setOnShoppinglistClick(Dash.this);
        }
        sharedRecycler.setAdapter(shpAdapter);

    }

    private void onEmptyClick() {

    }

    /**
     * Macht eine Datenbankverbindung und holt alle Shoppinglists die dem User gehören, diese werden auf dem recycled view angezeigt
     *
     * @param uid Die UserId damit von diesem user die shoppinglisten angezeigt werden
     */
    private void showOwnShoppingList(String uid) throws JSONException, SQLException {
        RecyclerView ownRecycleView = (RecyclerView) findViewById(R.id.ownrecycler);
        ownRecycleView.setHasFixedSize(true);
        ownRecycleView.setLayoutManager(new LinearLayoutManager(this));
        List<Shoppinglist> ownListsList = db.getMyShoppinglists(uid);
        ArrayList<Shoppinglist> ownListsArrayListTmp = new ArrayList<>();
        List<Shoppinglist> ownListsListTmp;
        View pfeil = findViewById(R.id.pfeilnachunten3);

        if (ownListsList.isEmpty()) {
            ownListsArrayListTmp.add(new Shoppinglist("empty", "Keine Shoppingliste vorhanden!", "Bitte eine Shoppingliste hinzufügen!", "empty", "#8B0000"));
            pfeil.setVisibility(View.VISIBLE);
            ownListsListTmp = ownListsArrayListTmp;
        } else {
            ownListsListTmp = ownListsList;
            pfeil.setVisibility(View.GONE);
        }

        ShoppinglistAdapter shpAdapter = new ShoppinglistAdapter(Dash.this, ownListsListTmp, db);
        if (!ownListsList.isEmpty()) {
            shpAdapter.setOnDelClick(Dash.this);
            shpAdapter.setOnChangeClick(Dash.this);
            shpAdapter.setOnShareClick(Dash.this);
            shpAdapter.setOnShoppinglistClick(Dash.this);
        } else {
            shpAdapter.setOnDelClick(new ShoppinglistAdapter.OnItemClicked() {
                @Override
                public void onItemClick(String sl_id) {

                }
            });
            shpAdapter.setOnChangeClick(new ShoppinglistAdapter.OnChangeItemClick() {
                @Override
                public void onChangeItemClick(String sl_id, View v) {

                }
            });
            shpAdapter.setOnShareClick(new ShoppinglistAdapter.OnShareClick() {
                @Override
                public void onShareClick(String sl_id, View v) {

                }
            });
            shpAdapter.setOnShoppinglistClick(new ShoppinglistAdapter.OnShoppinglistClick() {
                @Override
                public void onShoppinglistClick(String sl_id, View v) {

                }
            });
        }
        ownRecycleView.setAdapter(shpAdapter);

    }

    /**
     * Ist dafür Zuständig das es Tabs in der App gibt. Ohne dieser Funktion werden die Tabs nichtmehr Angezeigt.
     * Hier wird auch der Name der Tabs gesetzt
     */
    private void tabHoster(final String uid) {
        host = (TabHost) findViewById(R.id.tabHost1);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Eigene Einkaufslisten");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Eigene Einkaufslisten");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Geteilte Einkaufslisten");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Geteilte Einkaufslisten");
        host.addTab(spec);


        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {
                int i = host.getCurrentTab();

                View pfeil = findViewById(R.id.pfeilnachunten3);
                FloatingActionButton fab = findViewById(R.id.addShoppinglistFab);
                if (i == 0) {
                    try {
                        refreshOwnShoppinglist(uid);
                        fab.show();

                        if (db.getMyShoppinglists(uid).isEmpty()) {
                            pfeil.setVisibility(View.VISIBLE);
                        } else {
                            pfeil.setVisibility(View.GONE);
                        }
                        //TODO
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if (i == 1) {
                    try {
                        showSharedShoppingList(uid);
                        pfeil.setVisibility(View.GONE);
                        fab.hide();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    /**
     * Schickt an die Login Activity einen intend mit dem extra EXIT. Um die app zu schließen
     */
    private void exit() {
        finish();
        Intent intent = new Intent(Dash.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dash_menu, menu);
        return true;
    }

    /**
     * Menu item Action listener
     *
     * @param item Action Item
     * @return True wenn erfolgreich
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logoutBtn:
                logout();
                return true;

            case R.id.addInvite:
                popupaddInvite();
                return true;
            case R.id.doneEinkauf:
                finish();
                Intent intent = new Intent(Dash.this, DoneItemActivity.class);
                startActivity(intent);
                return true;
            case R.id.editUser:
                finish();
                Intent intent2 = new Intent(Dash.this, EditUser.class);
                startActivity(intent2);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Öffnet ein popup in dem ein invite link eingegeben werden kann. Diese Shoppingliste wird dann hinzugefügt
     */
    private void popupaddInvite() {
        final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupContentView = inflater.inflate(R.layout.add_share_link, null);

        final TextView linkEingabe = (TextView) popupContentView.findViewById(R.id.addShareLinkInput);

        ImageButton exitButton = (ImageButton) popupContentView.findViewById(R.id.addShareExit);
        Picasso.get().load(R.drawable.close).into(exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupAddShare.dismiss();
            }
        });
        final Button finish = (Button) popupContentView.findViewById(R.id.shareAddFinish);

        if (!linkEingabe.getText().toString().isEmpty()) {
            finish.setEnabled(true);
        } else {
            finish.setEnabled(false);
        }
        linkEingabe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!linkEingabe.getText().toString().isEmpty()) {
                    finish.setEnabled(true);
                } else {
                    finish.setEnabled(false);
                }
            }
        });

        popupAddShare = new PopupWindow(popupContentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String invite = linkEingabe.getText().toString();


                try {
                    db.addInviteLink(invite, FirebaseAuth.getInstance().getCurrentUser().getUid());
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                popupAddShare.dismiss();


                try {
                    TabHost tabhost = (TabHost) findViewById(R.id.tabHost1);
                    tabhost.setCurrentTab(1);
                    sharedswiperefresh.setRefreshing(true);
                    showSharedShoppingList(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    sharedswiperefresh.setRefreshing(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });


        popupAddShare.setOutsideTouchable(false);
        popupAddShare.setFocusable(true);
        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= 21) {
            popupAddShare.setElevation(5.0f);
        }
        popupAddShare.setAnimationStyle(R.style.popup_window_animation_phone);


        popupAddShare.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        popupAddShare.update();
    }

    /**
     * 2 Mal Zurück Drücken um die App zu schließen
     */
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            exit();

            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    /**
     * Das ist der Onclick für die einzelnen shoppinglists. Löscht eine shoppinglist und refreshed alle anderen
     *
     * @param sl_id Die Shoppingliste dieser Id wird gelöscht
     */
    private void onItemClickContainer(String sl_id) {
        try {
            db.delShoppinglist(sl_id);
            refreshOwnShoppinglist(FirebaseAuth.getInstance().getCurrentUser().getUid());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Das ist der oncklick für eine einzelen Shoppinglist. Bearbeitet eine Shoppinglist
     *
     * @param sl_id Die Shoppinglist die bearbeitet werden soll
     */
    private void onChangeItemClickContainer(String sl_id, View v) {
        try {
            showShoppinglistEditView(true, sl_id, "Shoppingliste bearbeiten", v);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onShoppinglistClickContainer(String sl_id) {
        finish();
        Intent intent = new Intent(this, ShoppinglistDetails.class);
        intent.putExtra("sl_id", sl_id);

        startActivity(intent);
    }

    /**
     * Das ist der Onclick für die einzelnen shoppinglists. Löscht eine shoppinglist und refreshed alle anderen
     *
     * @param sl_id Die Shoppingliste dieser Id wird gelöscht
     */
    @Override
    public void onItemClick(String sl_id) {
        onItemClickContainer(sl_id);
    }

    /**
     * Das ist der oncklick für eine einzelen Shoppinglist. Bearbeitet eine Shoppinglist
     *
     * @param sl_id Die Shoppinglist die bearbeitet werden soll
     */
    @Override
    public void onChangeItemClick(String sl_id, View v) {
        onChangeItemClickContainer(sl_id, v);
    }

    /**
     * Holt den Invitelink einer Shoppingliste
     *
     * @param sl_id Die Shoppingliste von der der invitelink gewünscht ist
     * @return
     */
//    private String getInviteLink(String sl_id, String invitelink, String dynamiclink) {
//
//        return link;
//    }
    @Override
    public void onShareClick(final String sl_id, final View v) {
        String link = null;
        try {
            if (db.isShared(sl_id)) {
                link = db.getInviteLink(sl_id);
                final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupContentView = inflater.inflate(R.layout.add_share, null);

                final TextView linkausgabe = (TextView) popupContentView.findViewById(R.id.shareLink);
                linkausgabe.setTextIsSelectable(true);
                linkausgabe.setText("invite.dergeorg.at/invite/" + link);

                ImageButton exitButton = (ImageButton) popupContentView.findViewById(R.id.shareExit);
                Picasso.get().load(R.drawable.close).into(exitButton);
                exitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupShare.dismiss();
                    }
                });

                ImageButton shareIntentBtn = (ImageButton) popupContentView.findViewById(R.id.shareIntentBtn);
                Picasso.get().load(R.drawable.share).into(shareIntentBtn);
                shareIntentBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String shoppinglistname = "";
                        try {
                            shoppinglistname = db.getShoppinglist(sl_id).getname();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Einladung zum bearbeiten der Shoppingliste " + shoppinglistname + " von: " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, linkausgabe.getText().toString());
                        popupShare.dismiss();
                        startActivity(Intent.createChooser(sharingIntent, "Teile mit"));
                    }
                });
                Button delShare = (Button) popupContentView.findViewById(R.id.delShare);

                final String finalLink = link;
                delShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Shoppinglist spl = null;
                        try {
                            spl = db.getShoppinglist(db.getSlIdFromInvite(finalLink));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        try {
                            MyFirebaseSender myFirebaseSender = new MyFirebaseSender(db.getMembers(sl_id));
                            myFirebaseSender.addMember(db.getAdmin(sl_id));
                            myFirebaseSender.sendMessage("Das Sharing von " + spl.getname() + " wurde von " + db.getUser(FirebaseAuth.getInstance().getCurrentUser().getUid()).getName() + " aufgehoben!", spl.getname() + " sharing wurde geändert!");
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            db.deleteInvite(finalLink);


                            TabHost tabhost = (TabHost) findViewById(R.id.tabHost1);
                            tabhost.setCurrentTab(0);
                            sharedswiperefresh.setRefreshing(true);

                            showSharedShoppingList(FirebaseAuth.getInstance().getCurrentUser().getUid());
                            sharedswiperefresh.setRefreshing(false);
                            popupShare.dismiss();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });

                popupShare = new PopupWindow(popupContentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                popupShare.setOutsideTouchable(false);
                popupShare.setFocusable(true);
                // Set an elevation value for popup window
                // Call requires API level 21
                if (Build.VERSION.SDK_INT >= 21) {
                    popupShare.setElevation(5.0f);
                }
                popupShare.setAnimationStyle(R.style.popup_window_animation_phone);


                popupShare.showAtLocation(v, Gravity.CENTER, 0, 0);
                popupShare.update();
            } else {
                final String invitelink = db.generateInviteLink();
                String url = "https://smartshopper.cf/androidinvite/" + sl_id;
                Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                        .setLink(Uri.parse("https://smartshopper.cf/invite/" + invitelink + "?slid=" + sl_id))
                        .setDomainUriPrefix("https://invite.dergeorg.at/invite")
                        .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                        .buildShortDynamicLink()
                        .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
                            @Override
                            public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                                if (task.isSuccessful()) {
                                    // Short link created
                                    final Uri shortLink = task.getResult().getShortLink();
                                    try {
                                        db.createInviteLink(sl_id, invitelink, db.getinviteFromLink(shortLink.toString()));
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                                    View popupContentView = inflater.inflate(R.layout.add_share, null);

                                    final TextView linkausgabe = (TextView) popupContentView.findViewById(R.id.shareLink);
                                    linkausgabe.setTextIsSelectable(true);
                                    try {
                                        linkausgabe.setText("invite.dergeorg.at/invite/" + db.getInviteLink(sl_id));
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    ImageButton exitButton = (ImageButton) popupContentView.findViewById(R.id.shareExit);
                                    Picasso.get().load(R.drawable.close).into(exitButton);
                                    exitButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            popupShare.dismiss();
                                        }
                                    });

                                    ImageButton shareIntentBtn = (ImageButton) popupContentView.findViewById(R.id.shareIntentBtn);
                                    Picasso.get().load(R.drawable.share).into(shareIntentBtn);
                                    shareIntentBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String shoppinglistname = "";
                                            try {
                                                shoppinglistname = db.getShoppinglist(sl_id).getname();
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                                            sharingIntent.setType("text/plain");
                                            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Einladung zum bearbeiten der Shoppingliste " + shoppinglistname + " von: " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, linkausgabe.getText().toString());
                                            popupShare.dismiss();
                                            startActivity(Intent.createChooser(sharingIntent, "Teile mit"));
                                        }
                                    });

//                                    final Button copyButton = (Button) popupContentView.findViewById(R.id.shareCopy);
//                                    copyButton.setOnClickListener(new View.OnClickListener() {
//                                        @Override
//                                        public void onClick(View v) {
//                                            copyText(linkausgabe.getText().toString());
//                                            popupShare.dismiss();
//                                        }
//                                    });

                                    Button delShare = (Button) popupContentView.findViewById(R.id.delShare);

                                    delShare.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Shoppinglist spl = null;
                                            try {
                                                spl = db.getShoppinglist(sl_id);
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            try {
                                                MyFirebaseSender myFirebaseSender = new MyFirebaseSender(db.getMembers(sl_id));
                                                myFirebaseSender.addMember(db.getAdmin(sl_id));
                                                myFirebaseSender.sendMessage("Das Sharing von " + spl.getname() + " wurde von " + db.getUser(FirebaseAuth.getInstance().getCurrentUser().getUid()).getName() + " aufgehoben!", spl.getname() + " sharing wurde geändert!");
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                            try {
                                                db.deleteInvite(db.getInviteLink(shortLink.toString()));


                                                TabHost tabhost = (TabHost) findViewById(R.id.tabHost1);
                                                tabhost.setCurrentTab(0);
                                                sharedswiperefresh.setRefreshing(true);

                                                showSharedShoppingList(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                sharedswiperefresh.setRefreshing(false);
                                                popupShare.dismiss();
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }


                                        }
                                    });

                                    popupShare = new PopupWindow(popupContentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    popupShare.setOutsideTouchable(false);
                                    popupShare.setFocusable(true);
                                    // Set an elevation value for popup window
                                    // Call requires API level 21
                                    if (Build.VERSION.SDK_INT >= 21) {
                                        popupShare.setElevation(5.0f);
                                    }
                                    popupShare.setAnimationStyle(R.style.popup_window_animation_phone);


                                    popupShare.showAtLocation(v, Gravity.CENTER, 0, 0);
                                    popupShare.update();
                                    Uri flowchartLink = task.getResult().getPreviewLink();
                                } else {
                                    // Error
                                    // ...
                                }
                            }
                        });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Kopiert einen Text in die Zwischenablage
     *
     * @param text Der Text, welcher zu kopieren ist
     */
    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("SmartShopper", text);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onShoppinglistClick(String sl_id, View v) {
        onShoppinglistClickContainer(sl_id);
    }

    @Override
    public void sharedOnItemClick(String sl_id) {
        onItemClickContainer(sl_id);
    }

    @Override
    public void sharedOnChangeItemClick(String sl_id, View v) {
        onChangeItemClickContainer(sl_id, v);
    }

    @Override
    public void sharedOnShareClick(final String sl_id, View v) throws SQLException, JSONException {
        final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupContentView = inflater.inflate(R.layout.edit_share_member, null);

        ImageButton exitBtn = popupContentView.findViewById(R.id.exitButton);
        Picasso.get().load(R.drawable.close).into(exitBtn);
        final TextView linkAusgabe = popupContentView.findViewById(R.id.linkausgabe);
//        Button copyBtn = popupContentView.findViewById(R.id.copyButton);
        Button stopShareBtn = popupContentView.findViewById(R.id.delShare);

        linkAusgabe.setTextIsSelectable(true);
        linkAusgabe.setText("invite.dergeorg.at/invite/" + db.getInviteLink(sl_id));
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupEditShare.dismiss();
            }
        });
        ImageButton shareIntentBtn = popupContentView.findViewById(R.id.shareIntentBtn);
        Picasso.get().load(R.drawable.share).into(shareIntentBtn);
        shareIntentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shoppinglistname = "";
                try {
                    shoppinglistname = db.getShoppinglist(sl_id).getname();
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Einladung zum bearbeiten der Shoppingliste " + shoppinglistname + " von: " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, linkAusgabe.getText().toString());
                startActivity(Intent.createChooser(sharingIntent, "Teilen mit"));
            }
        });
        stopShareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ownswiperefresh.setRefreshing(true);
                    db.stopInvite(linkAusgabe.getText().toString(), FirebaseAuth.getInstance().getCurrentUser().getUid());
                    popupEditShare.dismiss();
                    showSharedShoppingList(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    ownswiperefresh.setRefreshing(false);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        popupEditShare = new PopupWindow(popupContentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupEditShare.setOutsideTouchable(false);
        popupEditShare.setFocusable(true);
        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= 21) {
            popupEditShare.setElevation(5.0f);
        }
        popupEditShare.setAnimationStyle(R.style.popup_window_animation_phone);


        popupEditShare.showAtLocation(v, Gravity.CENTER, 0, 0);
        popupEditShare.update();
    }

    @Override
    public void sharedOnShoppinglistClick(String sl_id, View v) {
        onShoppinglistClickContainer(sl_id);
    }
}
