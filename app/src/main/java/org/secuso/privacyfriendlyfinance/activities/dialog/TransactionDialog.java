/*
 This file is part of Privacy Friendly App Finance Manager.

 Privacy Friendly App Finance Manager is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Finance Manager is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Finance Manager. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlyfinance.activities.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.secuso.privacyfriendlyfinance.R;
import org.secuso.privacyfriendlyfinance.activities.adapter.NullableArrayAdapter;
import org.secuso.privacyfriendlyfinance.activities.helper.CurrencyInputFilter;
import org.secuso.privacyfriendlyfinance.activities.viewmodel.TransactionDialogViewModel;
import org.secuso.privacyfriendlyfinance.databinding.DialogTransactionBinding;
import org.secuso.privacyfriendlyfinance.domain.model.Account;
import org.secuso.privacyfriendlyfinance.domain.model.Category;
import org.secuso.privacyfriendlyfinance.domain.model.RepeatingTransaction;
import org.secuso.privacyfriendlyfinance.domain.model.Transaction;

import java.util.List;

/**
 * Dialog for adding new transactions and for editing existing transactions.
 *
 * @author Felix Hofmann, Leonard Otto
 */
public class TransactionDialog extends AppCompatDialogFragment {
    public static final String EXTRA_CATEGORY_ID = "org.secuso.privacyfriendlyfinance.EXTRA_CATEGORY_ID";
    public static final String EXTRA_ACCOUNT_ID = "org.secuso.privacyfriendlyfinance.EXTRA_ACCOUNT_ID";
    public static final String EXTRA_TRANSACTION_ID = "org.secuso.privacyfriendlyfinance.EXTRA_TRANSACTION_ID";

    private AlertDialog dialog;
    private View view;
    private EditText editTextAmount;
    private TextView editTextDate;
    private Spinner categorySpinner;
    private Spinner accountSpinner;
    private TextView tvRepeating;
    private ImageView ivRepeating;

    private TransactionDialogViewModel viewModel;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        viewModel = ViewModelProviders.of(this).get(TransactionDialogViewModel.class);

        final DialogTransactionBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_transaction, null, false);
        view = binding.getRoot();
        builder.setView(view);

        editTextAmount = view.findViewById(R.id.dialog_transaction_amount);
        editTextDate = view.findViewById(R.id.dialog_transaction_date);
        categorySpinner = view.findViewById(R.id.category_spinner);
        accountSpinner = view.findViewById(R.id.account_spinner);
        tvRepeating = view.findViewById(R.id.textView_repeating);
        ivRepeating = view.findViewById(R.id.imageView_repeating);
        viewModel.setCurrencyColors(getResources().getColor(R.color.green), getResources().getColor(R.color.red));

        long transactionId = getArguments().getLong(EXTRA_TRANSACTION_ID, -1L);

        if (transactionId >= 0) {
            builder.setTitle(R.string.dialog_transaction_edit_title);
        } else {
            builder.setTitle(R.string.dialog_transaction_create_title);
        }
        if (viewModel.getTransaction() == null) {
            viewModel.setTransactionId(transactionId).observe(this, new Observer<Transaction>() {
                @Override
                public void onChanged(@Nullable Transaction transaction) {
                    // Is it a transaction dummy?
                    if (transaction.getId() == null) {
                        Log.d("acc id", getArguments().getLong(EXTRA_ACCOUNT_ID, -1L) + "");
                        transaction.setAccountId(getArguments().getLong(EXTRA_ACCOUNT_ID, -1L));
                        transaction.setCategoryId(getArguments().getLong(EXTRA_CATEGORY_ID, -1L));
                    }
                    viewModel.setTransaction(transaction);
                    bindRepeatingTransaction();
                    binding.setViewModel(viewModel);
                }
            });
        } else {
            bindRepeatingTransaction();
            binding.setViewModel(viewModel);
        }

        viewModel.getAllAccounts().observe(this, new Observer<List<Account>>() {
            @Override
            public void onChanged(@Nullable List<Account> accounts) {
                accountSpinner.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, accounts));
                accountSpinner.setSelection(viewModel.getAccountIndex());
            }
        });

        viewModel.getAllCategories().observe(this, new Observer<List<Category>>() {
            @Override
            public void onChanged(@Nullable List<Category> categories) {
                categorySpinner.setAdapter(new NullableArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, categories));
                categorySpinner.setSelection(viewModel.getCategoryIndex());
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                viewModel.cancel();
            }
        });

        builder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                viewModel.submit();
            }
        });

        editTextAmount.setFilters(new InputFilter[] {new CurrencyInputFilter()});

        editTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePickerTransactionDate();
            }
        });

        dialog = builder.create();
        return dialog;
    }

    private void openDatePickerTransactionDate() {
        openDatePicker(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                viewModel.setDate(new LocalDate(year, month + 1, dayOfMonth));
            }
        });
    }

    private void bindRepeatingTransaction() {
        if (viewModel.getRepeatingTransaction() != null) {
            viewModel.getRepeatingTransaction().observe(this, new Observer<RepeatingTransaction>() {
                @Override
                public void onChanged(@Nullable RepeatingTransaction repeatingTransaction) {
                    if (repeatingTransaction == null) {
                        tvRepeating.setVisibility(View.INVISIBLE);
                        ivRepeating.setVisibility(View.INVISIBLE);
                    } else {
                        tvRepeating.setText(repeatingTransaction.getName());
                        tvRepeating.setVisibility(View.VISIBLE);
                        ivRepeating.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            tvRepeating.setVisibility(View.INVISIBLE);
            ivRepeating.setVisibility(View.INVISIBLE);
        }
    }

    private void openDatePicker(DatePickerDialog.OnDateSetListener listener) {
        LocalDate date = viewModel.getDate();
        new DatePickerDialog(getContext(), listener, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth()).show();
        dialog.show();
    }
}
