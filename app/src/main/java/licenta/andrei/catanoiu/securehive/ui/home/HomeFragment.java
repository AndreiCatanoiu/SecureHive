package licenta.andrei.catanoiu.securehive.ui.home;

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

import licenta.andrei.catanoiu.securehive.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ArrayList<String> devices;
    private ArrayAdapter<String> adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ListView listView = binding.listDevices;
        TextView emptyView = binding.textEmpty;

        devices = new ArrayList<>();

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, devices);
        listView.setAdapter(adapter);
        updateVisibility();

        return root;
    }

    private void updateVisibility() {
        if (devices.isEmpty()) {
            binding.listDevices.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.listDevices.setVisibility(View.VISIBLE);
            binding.textEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
