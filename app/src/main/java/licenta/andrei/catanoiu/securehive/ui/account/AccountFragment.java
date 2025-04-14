package licenta.andrei.catanoiu.securehive.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import licenta.andrei.catanoiu.securehive.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setează acțiunile pentru butoane
        binding.buttonEditProfile.setOnClickListener(v -> {
            // Aici adaugi logica de editare a profilului
        });

        binding.buttonAddDevices.setOnClickListener(v -> {
            // Aici adaugi logica de adăugare dispozitive
        });

        binding.buttonLogout.setOnClickListener(v -> {
            // Aici adaugi logica de logout
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
