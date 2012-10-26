package com.richitec.imeeting.assistant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.richitec.commontoolkit.addressbook.AddressBookManager;
import com.richitec.commontoolkit.addressbook.ContactBean;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.imeeting.R;
import com.richitec.imeeting.account.AccountSettingActivity;
import com.richitec.imeeting.constants.CloudAddressBookConstants;
import com.richitec.imeeting.customcomponent.IMeetingNavigationActivity;

public class SettingActivity extends IMeetingNavigationActivity {
	private ProgressDialog progressDlg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content view
		setContentView(R.layout.setting_activity_layout2);

		// set title text
		setTitle(R.string.setting_nav_title_text);

		// bind account setting button on click listener
		/*((Button) findViewById(R.id.accountSetting_btn))
				.setOnClickListener(new AccountSettingBtnOnClickListener());
		

		// bind help button on click listener
		((Button) findViewById(R.id.help_btn))
				.setOnClickListener(new HelpBtnOnClickListener());

		// bind about button on click listener
		((Button) findViewById(R.id.about_btn))
				.setOnClickListener(new AboutBtnOnClickListener());*/
		
		((LinearLayout)findViewById(R.id.accountSetting_item))
				.setOnClickListener(new AccountSettingBtnOnClickListener());
		((LinearLayout)findViewById(R.id.about_item))
		.setOnClickListener(new AboutBtnOnClickListener());
		((LinearLayout)findViewById(R.id.help_item))
		.setOnClickListener(new HelpBtnOnClickListener());
		

	}

	// inner class
	// account setting button on click listener
	class AccountSettingBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// go to user account setting activity
			pushActivity(AccountSettingActivity.class);
		}

	}

	// help button on click listener
	class HelpBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// go to help activity
			pushActivity(SupportActivity.class);
		}

	}

	// about button on click listener
	class AboutBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// go to about activity
			pushActivity(AboutActivity.class);
		}

	}

	public void onAccountChargeButtonClick(View v) {
		pushActivity(AccountChargeActivity.class);
	}

	public void onAddressBookUploadButtonClick(View v) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.addressbook_upload_btn_title)
				.setMessage(R.string.need_upload_addressbook)
				.setPositiveButton(R.string.backup,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								progressDlg = ProgressDialog
										.show(SettingActivity.this,
												null,
												getString(R.string.uploading_address_book));
								new Thread(new Runnable() {

									@Override
									public void run() {
										uploadAddressBook();
									}
								}).start();
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}

	private void uploadAddressBook() {
		String accountName = UserManager.getInstance().getUser().getName();

		AddressBookManager abm = AddressBookManager.getInstance();
		List<ContactBean> contacts = abm.getAllContactsInfoArray();
		JSONArray contactArray = new JSONArray();

		for (ContactBean contact : contacts) {
			StringBuffer searchName = new StringBuffer();
			List<String> fullNames = contact.getFullNames();
			if (fullNames != null) {
				for (String name : fullNames) {
					searchName.append(name);
				}
			}

			JSONObject contactJson = new JSONObject();
			try {
				contactJson.put(CloudAddressBookConstants.owner.name(),
						accountName);
				contactJson.put(CloudAddressBookConstants.display_name.name(),
						contact.getDisplayName());

				contactJson.put(
						CloudAddressBookConstants.phonetic_array.name(),
						new JSONArray(contact.getNamePhonetics() != null ? contact.getNamePhonetics() : new ArrayList<String>()));
				contactJson.put(CloudAddressBookConstants.name_array.name(),
						new JSONArray(contact.getFullNames() != null ? contact.getFullNames() : new ArrayList<String>()));
				contactJson.put(CloudAddressBookConstants.search_name.name(),
						searchName.toString());
				contactJson.put(CloudAddressBookConstants.phone_array.name(),
						new JSONArray(contact.getPhoneNumbers() != null ? contact.getPhoneNumbers() : new ArrayList<String>()));

			} catch (JSONException e) {
				e.printStackTrace();
			}
			contactArray.put(contactJson);
		}
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("contacts", contactArray.toString());
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.upload_addressbook_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedUploadAddressBook);
	}

	private OnHttpRequestListener onFinishedUploadAddressBook = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			new AlertDialog.Builder(SettingActivity.this)
					.setTitle(R.string.alert_title)
					.setMessage(R.string.addressbook_upload_ok)
					.setPositiveButton(R.string.ok, null).show();
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			new AlertDialog.Builder(SettingActivity.this)
					.setTitle(R.string.alert_title)
					.setMessage(R.string.addressbook_upload_fail)
					.setPositiveButton(R.string.ok, null).show();
		}
	};

	private void dismissProgressDlg() {
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}
}
