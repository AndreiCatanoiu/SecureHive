package licenta.andrei.catanoiu.securehive.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.NavOptions;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.utils.NotificationService;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationService.createNotificationChannel(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_alerts, R.id.navigation_account)
                .build();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        String targetFragment = getIntent().getStringExtra("fragment");
        if (targetFragment != null) {
            switch (targetFragment) {
                case "alerts":
                    navController.navigate(R.id.navigation_alerts, null,
                        new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_home, false)
                            .build());
                    break;
                case "devices":
                    navController.navigate(R.id.navigation_home, null,
                        new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_alerts, false)
                            .build());
                    break;
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (navController == null) return;
        String targetFragment = intent.getStringExtra("fragment");
        if (targetFragment != null) {
            switch (targetFragment) {
                case "alerts":
                    navController.navigate(R.id.navigation_alerts, null,
                        new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_home, false)
                            .build());
                    break;
                case "devices":
                    navController.navigate(R.id.navigation_home, null,
                        new NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_alerts, false)
                            .build());
                    break;
            }
        }
    }
}