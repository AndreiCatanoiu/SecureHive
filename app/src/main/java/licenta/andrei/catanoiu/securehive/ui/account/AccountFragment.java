package licenta.andrei.catanoiu.securehive.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

private FragmentAccountBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel dashboardViewModel =
                new ViewModelProvider(this).get(AccountViewModel.class);

    binding = FragmentAccountBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

        final TextView textView = binding.textAccount;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}