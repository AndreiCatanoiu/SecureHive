package licenta.andrei.catanoiu.securehive.fragments.alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import licenta.andrei.catanoiu.securehive.databinding.FragmentAlertsBinding;

public class AlertsFragment extends Fragment {

    private FragmentAlertsBinding binding;
    private ArrayList<String> alerts;
    private ArrayAdapter<String> adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ListView listView = binding.listAlerts;
        TextView emptyView = binding.textEmptyAlerts;

        listView.setEmptyView(emptyView);

        alerts = new ArrayList<>();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, alerts);
        listView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
