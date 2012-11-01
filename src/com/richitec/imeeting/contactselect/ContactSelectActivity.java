package com.richitec.imeeting.contactselect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.richitec.commontoolkit.activityextension.AppLaunchActivity;
import com.richitec.commontoolkit.addressbook.AddressBookManager;
import com.richitec.commontoolkit.addressbook.ContactBean;
import com.richitec.commontoolkit.customadapter.CommonListAdapter;
import com.richitec.commontoolkit.customcomponent.BarButtonItem.BarButtonItemStyle;
import com.richitec.commontoolkit.customcomponent.CommonPopupWindow;
import com.richitec.commontoolkit.customcomponent.ListViewQuickAlphabetBar;
import com.richitec.commontoolkit.customcomponent.ListViewQuickAlphabetBar.OnTouchListener;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.commontoolkit.utils.MyToast;
import com.richitec.commontoolkit.utils.StringUtils;
import com.richitec.imeeting.R;
import com.richitec.imeeting.constants.SystemConstants;
import com.richitec.imeeting.constants.TalkGroup;
import com.richitec.imeeting.customcomponent.IMeetingBarButtonItem;
import com.richitec.imeeting.customcomponent.IMeetingNavigationActivity;
import com.richitec.imeeting.service.ContactSyncService;
import com.richitec.imeeting.talkinggroup.TalkingGroupActivity;

public class ContactSelectActivity extends IMeetingNavigationActivity {
	private ProgressDialog progressDialog;
	private static final String LOG_TAG = "ContactSelectActivity";

	// contact select activity onCreate param key
	public static final String CONTACT_SELECT_ACTIVITY_PARAM_TALKINGGROUPSTATUS = "talking group status";

	// in address book is selected flag saved in contact bean extension
	// structured and in address book contact adapter data key
	private final String CONTACT_IS_SELECTED = "in address book contact is selected";

	// selected contact the selected phone which saved in contact bean extension
	// structured
	private final String SELECTED_CONTACT_SELECTEDPHONE = "selected contact the selected phone";

	// in and prein talking group contacts adapter data keys
	private final String SELECTED_CONTACT_DISPLAYNAME = "selected_contact_displayName";
	private final String SELECTED_CONTACT_IS_IN_TALKINGGROUP = "selected_contact_is_in_talkingGroup";

	// current talking group status, default value is establishing
	private TalkingGroupStatus _mTalkingGroupStatus = TalkingGroupStatus.ESTABLISHING;

	// talking group id
	private String _mTalkingGroupId;
	// talking group attendees phone list
	private List<String> _mTalkingGroupContactsPhoneArray = new ArrayList<String>();

	// address book contacts list view
	private ListView _mABContactsListView;

	// all address book name phonetic sorted contacts detail info list
	private final List<ContactBean> allNamePhoneticSortedContactsInfoArray = AddressBookManager
			.getInstance().getAllNamePhoneticSortedContactsInfoArray();
	// present contacts in address book detail info list
	private List<ContactBean> _mPresentContactsInABInfoArray;

	// contact search status
	private ContactSearchStatus _mContactSearchStatus = ContactSearchStatus.NONESEARCH;

	// define add manual input contact popup window
	private final AddManualInputContactPopupWindow _mAddManualInputContactPopupWindow = new AddManualInputContactPopupWindow(
			R.layout.add_manualinput_contact_popupwindow_layout,
			LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

	// define contact phone numbers select popup window
	private final ContactPhoneNumbersSelectPopupWindow _mContactPhoneNumbersSelectPopupWindow = new ContactPhoneNumbersSelectPopupWindow(
			R.layout.contact_phonenumbers_select_layout,
			LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
	
	private final AddAreaCodePopupWindow _mAddAreaCodePopupWindow = new AddAreaCodePopupWindow(
			R.layout.add_phone_areacode_contact_popupwindow_layout,
			LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
	
	private final WrongPhoneFormatInformPopupWindow _mWrongPhoneFormatInformPopupWindow = new WrongPhoneFormatInformPopupWindow(
			R.layout.wrong_phone_format_popupwindow_layout,
			LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);

	// in and prein talking group contacts list view
	private ListView _mIn7PreinTalkingGroupContactsListView;

	// prein talking group contacts detail info list
	private final List<ContactBean> _mPreinTalkingGroupContactsInfoArray = new ArrayList<ContactBean>();

	// in and prein talking group contacts adapter data list
	private final List<Map<String, ?>> _mIn7PreinTalkingGroupContactsAdapterDataList = new ArrayList<Map<String, ?>>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//change contact DB observer handler to custom handler for update the Listview UI 
		//AddressBookManager.getInstance().registContactObserverOrChangeHandler(new UpdateABListHandler());
		ContactSyncService.setHandler(new UpdateABListHandler());

		// get the intent parameter data
		Bundle _data = getIntent().getExtras();

		// check the data bundle
		if (null != _data) {
			// set talking group status
			_mTalkingGroupStatus = (TalkingGroupStatus) _data
					.get(CONTACT_SELECT_ACTIVITY_PARAM_TALKINGGROUPSTATUS);
			if (_mTalkingGroupStatus == null) {
				_mTalkingGroupStatus = TalkingGroupStatus.ESTABLISHING;
			}
			// set talking group id and attendees phone list
			_mTalkingGroupId = (String) _data
					.get(TalkingGroupActivity.TALKINGGROUP_ACTIVITY_PARAM_TALKINGGROUPID);
			@SuppressWarnings("unchecked")
			List<String> _attendeesPhoneList = (List<String>) _data
					.get(TalkingGroupActivity.TALKINGGROUP_ACTIVITY_PARAM_TALKINGGROUP_ATTENDEESPHONE);
			if (null != _attendeesPhoneList) {
				_mTalkingGroupContactsPhoneArray.addAll(_attendeesPhoneList);
			}
		}

		// set content view
		setContentView(R.layout.contact_select_activity_layout);

		// set title text
		setTitle(R.string.contactSelect_nav_title_text);

		// set nav back bar button item as self activity left bar button item
		setLeftBarButtonItem(new IMeetingBarButtonItem(this,
				BarButtonItemStyle.LEFT_BACK, R.string.back_nav_btn_title,
				new NavBackBtnOnClickListener()));

		// check current talking group status
		if (TalkingGroupStatus.GOING == _mTalkingGroupStatus) {
			// set confirm add new contacts to talking group bar button item as
			// self activity right bar button item
			setRightBarButtonItem(new IMeetingBarButtonItem(this,
					BarButtonItemStyle.RIGHT_GO,
					R.string.confirmAdd_nav_btn_title,
					new ConfirmAddNewContacts2TalkingGroupBtnOnClickListener()));
		}
		// establishing
		else {
			// set open talking group bar button item as self activity right bar
			// button item
			setRightBarButtonItem(new IMeetingBarButtonItem(this,
					BarButtonItemStyle.RIGHT_GO,
					R.string.openTalkingGroup_nav_btn_title,
					new OpenTalkingGroupBtnOnClickListener()));
		}

		// init present contacts in address book detail info array
		_mPresentContactsInABInfoArray = allNamePhoneticSortedContactsInfoArray;

		// add moderator to talking group attendees list as header
		_mTalkingGroupContactsPhoneArray.add(0, UserManager.getInstance()
				.getUser().getName());

		// init contacts in address book list view
		_mABContactsListView = (ListView) findViewById(R.id.contactInAB_listView);

		// set contacts in address book listView adapter
		_mABContactsListView
				.setAdapter(generateInABContactAdapter(_mPresentContactsInABInfoArray));
		// init address book contacts listView quick alphabet bar and add on
		// touch listener
		new ListViewQuickAlphabetBar(_mABContactsListView)
				.setOnTouchListener(new ContactsInABListViewQuickAlphabetBarOnTouchListener());

		// bind contacts in address book listView item click listener
		_mABContactsListView
				.setOnItemClickListener(new ContactsInABListViewOnItemClickListener());

		// bind contact search editText text watcher
		((EditText) findViewById(R.id.contact_search_editText))
				.addTextChangedListener(new ContactSearchEditTextTextWatcher());

		// bind add manual input contact button on click listener
		((Button) findViewById(R.id.add_manualInputContact_btn))
				.setOnClickListener(new AddManualInputContactBtnOnClickListener());

		// init contacts in address book list view
		_mIn7PreinTalkingGroupContactsListView = (ListView) findViewById(R.id.contactIn7PreinTalkingGroup_listView);
		LayoutParams params = _mIn7PreinTalkingGroupContactsListView
				.getLayoutParams();
		Display display = getWindowManager().getDefaultDisplay();
		params.width = (int) (display.getWidth() * 0.33);
		_mIn7PreinTalkingGroupContactsListView.setLayoutParams(params);
		// generate in and prein talking group contact adapter
		// process in talking group attendees phone list, then set in and prein
		// talking group contacts list view present data list
		for (int i = 0; i < _mTalkingGroupContactsPhoneArray.size(); i++) {
			// add data to list
			_mIn7PreinTalkingGroupContactsAdapterDataList
					.add(generateIn6PreinTalkingGroupAdapterData(
							_mTalkingGroupContactsPhoneArray.get(i), true));
		}

		// set contacts in and prein talking group listView adapter
		_mIn7PreinTalkingGroupContactsListView
				.setAdapter(new InAB6In7PreinTalkingGroupContactAdapter(
						this,
						_mIn7PreinTalkingGroupContactsAdapterDataList,
						R.layout.in7prein_talking_group_contact_layout,
						new String[] { SELECTED_CONTACT_DISPLAYNAME,
								SELECTED_CONTACT_IS_IN_TALKINGGROUP },
						new int[] {
								R.id.in7preinTalkingGroup_contact_displayName_textView,
								R.id.in7preinTalkingGroup_contactInTalkingGroup_imageView }));

		// bind contacts in and prein talking group listView item click listener
		_mIn7PreinTalkingGroupContactsListView
				.setOnItemClickListener(new ContactsIn7PreinTalkingGroupListViewOnItemClickListener());
	}

	// generate in address book contact adapter
	private ListAdapter generateInABContactAdapter(
			List<ContactBean> presentContactsInAB) {
		
		// in address book contacts adapter data keys
		final String PRESENT_CONTACT_NAME = "present_contact_name";
		final String PRESENT_CONTACT_PHONES = "contact_phones";
		// set address book contacts list view present data list
		List<Map<String, ?>> _addressBookContactsPresentDataList = new ArrayList<Map<String, ?>>();

		for (ContactBean _contact : presentContactsInAB) {
			// generate data
			HashMap<String, Object> _dataMap = new HashMap<String, Object>();

			// get contact name and phone matching indexes
			@SuppressWarnings("unchecked")
			SparseArray<Integer> _nameMatchingIndexes = (SparseArray<Integer>) _contact
					.getExtension().get(
							AddressBookManager.NAME_MATCHING_INDEXES);
			@SuppressWarnings("unchecked")
			List<List<Integer>> _phoneMatchingIndexes = (List<List<Integer>>) _contact
					.getExtension().get(
							AddressBookManager.PHONENUMBER_MATCHING_INDEXES);

			// set data
			if (ContactSearchStatus.SEARCHBYNAME == _mContactSearchStatus
					|| ContactSearchStatus.SEARCHBYCHINESENAME == _mContactSearchStatus) {
				// get display name
				SpannableString _displayName = new SpannableString(
						_contact.getDisplayName());

				// set attributed
				for (int i = 0; i < _nameMatchingIndexes.size(); i++) {
					// get key and value
					Integer _nameCharMatchedPos = getRealPositionInContactDisplayName(
							_contact.getDisplayName(),
							_nameMatchingIndexes.keyAt(i));
					Integer _nameCharMatchedLength = _nameMatchingIndexes
							.get(_nameMatchingIndexes.keyAt(i));

					_displayName
							.setSpan(
									new ForegroundColorSpan(Color.BLUE),
									_nameCharMatchedPos,
									AddressBookManager.NAME_CHARACTER_FUZZYMATCHED_LENGTH == _nameCharMatchedLength ? _nameCharMatchedPos + 1
											: _nameCharMatchedPos
													+ _nameCharMatchedLength,
									Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				_dataMap.put(PRESENT_CONTACT_NAME, _displayName);
			} else {
				_dataMap.put(PRESENT_CONTACT_NAME, _contact.getDisplayName());
			}
			if (ContactSearchStatus.SEARCHBYPHONE == _mContactSearchStatus) {
				// get format phone number string
				SpannableString _formatPhoneNumberString = new SpannableString(
						_contact.getFormatPhoneNumbers());

				// get format phone number string separator "\n" positions
				List<Integer> _sepPositions = StringUtils.subStringPositions(
						_contact.getFormatPhoneNumbers(), "\n");

				// set attributed
				for (int i = 0; i < _phoneMatchingIndexes.size(); i++) {
					// check the phone matched
					if (0 != _phoneMatchingIndexes.get(i).size()) {
						// get begin and end position
						int _beginPos = _phoneMatchingIndexes.get(i).get(0);
						int _endPos = _phoneMatchingIndexes.get(i).get(
								_phoneMatchingIndexes.get(i).size() - 1) + 1;

						// check matched phone
						if (1 <= i) {
							_beginPos += _sepPositions.get(i - 1) + 1;
							_endPos += _sepPositions.get(i - 1) + 1;
						}

						_formatPhoneNumberString.setSpan(
								new ForegroundColorSpan(Color.BLUE), _beginPos,
								_endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}

				_dataMap.put(PRESENT_CONTACT_PHONES, _formatPhoneNumberString);
			} else {
				_dataMap.put(PRESENT_CONTACT_PHONES,
						_contact.getFormatPhoneNumbers());
			}

			// put alphabet index
			_dataMap.put(CommonListAdapter.ALPHABET_INDEX,
					_contact.getNamePhoneticsString());

			// get in address book contact is selected flag saved in contact
			// bean extension structured
			Boolean _isSelected = (Boolean) _contact.getExtension().get(
					CONTACT_IS_SELECTED);
			if (null == _isSelected) {
				_contact.getExtension().put(CONTACT_IS_SELECTED, false);
			}
			_dataMap.put(CONTACT_IS_SELECTED,
					_contact.getExtension().get(CONTACT_IS_SELECTED));

			// add data to list
			_addressBookContactsPresentDataList.add(_dataMap);
		}

		// get address book contacts listView adapter
		InAB6In7PreinTalkingGroupContactAdapter _addressBookContactsListViewAdapter = (InAB6In7PreinTalkingGroupContactAdapter) _mABContactsListView
				.getAdapter();

		return null == _addressBookContactsListViewAdapter ? new InAB6In7PreinTalkingGroupContactAdapter(
				this,
				_addressBookContactsPresentDataList,
				R.layout.addressbook_contact_layout,
				new String[] { PRESENT_CONTACT_NAME, PRESENT_CONTACT_PHONES,
						CONTACT_IS_SELECTED },
				new int[] {
						R.id.adressBook_contact_displayName_textView,
						R.id.addressBook_contact_phoneNumber_textView,
						R.id.addressBook_contact_unsel6sel_imageView_parentFrameLayout })
				: _addressBookContactsListViewAdapter
						.setData(_addressBookContactsPresentDataList);
	}

	// generate in or prein talking group adapter data
	private Map<String, Object> generateIn6PreinTalkingGroupAdapterData(
			String displayName6Phone, boolean isInTalkingGroup) {
		Map<String, Object> _dataMap = new HashMap<String, Object>();

		// set data
		_dataMap.put(
				SELECTED_CONTACT_DISPLAYNAME,
				isInTalkingGroup ? AddressBookManager.getInstance()
						.getContactsDisplayNamesByPhone(displayName6Phone)
						.get(0) : displayName6Phone);
		_dataMap.put(SELECTED_CONTACT_IS_IN_TALKINGGROUP, isInTalkingGroup);

		return _dataMap;
	}

	// get real position in contact display name with original position
	private Integer getRealPositionInContactDisplayName(String displayName,
			Integer origPosition) {
		int _realPos = 0;

		int _tmpPos = 0;
		boolean _prefixHasChar = false;

		for (int i = 0; i < displayName.length(); i++) {
			if (String.valueOf(displayName.charAt(i))
					.matches("[\u4e00-\u9fa5]")) {
				if (_prefixHasChar) {
					_prefixHasChar = false;

					_tmpPos += 1;
				}

				if (_tmpPos == origPosition) {
					_realPos = i;

					break;
				}

				_tmpPos += 1;
			} else if (' ' == displayName.charAt(i)) {
				if (_prefixHasChar) {
					_prefixHasChar = false;

					_tmpPos += 1;
				}
			} else {
				if (_tmpPos == origPosition) {
					_realPos = i;

					break;
				}

				_prefixHasChar = true;
			}
		}

		return _realPos;
	}

	// mark contact selected
	private void markContactSelected(String selectedPhone, int contactPosition,
			boolean isPresentInABContactsListView) {
		// get selected contact object
		ContactBean _selectedContact;

		// check if it is present in address book contacts listView
		if (isPresentInABContactsListView) {
			_selectedContact = _mPresentContactsInABInfoArray
					.get(contactPosition);
		} else {
			_selectedContact = allNamePhoneticSortedContactsInfoArray
					.get(contactPosition);
		}

		// check the selected contact is in talking group attendees
		if (_mTalkingGroupContactsPhoneArray.contains(selectedPhone)) {
			MyToast.show(
					ContactSelectActivity.this,
					AddressBookManager.getInstance()
							.getContactsDisplayNamesByPhone(selectedPhone)
							.get(0)
							+ ContactSelectActivity.this
									.getResources()
									.getString(
											R.string.toast_selectedContact_existedInTalkingGroup_attendees),
					Toast.LENGTH_SHORT);

			return;
		}
		
		//selectedPhone = checkPhoneFormatAndChange(selectedPhone,true,_selectedContact);

		// set selected contact the selected phone
		Log.d("debug mark selected phone ", selectedPhone);
		
		_selectedContact.getExtension().put(SELECTED_CONTACT_SELECTEDPHONE,
				selectedPhone);

		// update contact is selected flag
		_selectedContact.getExtension().put(CONTACT_IS_SELECTED, true);

		// update address book contacts listView, if the selected contact is
		// present in address book contacts listView
		if (isPresentInABContactsListView) {
			// get in address book present contacts adapter
			InAB6In7PreinTalkingGroupContactAdapter _inABContactAdapter = (InAB6In7PreinTalkingGroupContactAdapter) _mABContactsListView
					.getAdapter();

			// get in address book present contacts adapter data map
			@SuppressWarnings("unchecked")
			Map<String, Object> _inABContactAdapterDataMap = (Map<String, Object>) _inABContactAdapter
					.getItem(contactPosition);

			// update address book present contacts adapter data map and notify
			// adapter changed
			_inABContactAdapterDataMap.put(CONTACT_IS_SELECTED,
					_selectedContact.getExtension().get(CONTACT_IS_SELECTED));
			_inABContactAdapter.notifyDataSetChanged();
		}

		// add to in and prein talking group contacts adapter data list and
		// notify adapter changed
		_mPreinTalkingGroupContactsInfoArray.add(_selectedContact);
		_mIn7PreinTalkingGroupContactsAdapterDataList
				.add(generateIn6PreinTalkingGroupAdapterData(
						_selectedContact.getDisplayName(), false));
		((InAB6In7PreinTalkingGroupContactAdapter) _mIn7PreinTalkingGroupContactsListView
				.getAdapter()).notifyDataSetChanged();
	}

	// mark contact unselected
	private void markContactUnselected(int contactPosition,
			boolean isClickedOnABContactsListView) {
		// get the selected contact
		ContactBean _selectedContact;
		if (isClickedOnABContactsListView) {
			_selectedContact = _mPresentContactsInABInfoArray
					.get(contactPosition);
		} else {
			_selectedContact = _mPreinTalkingGroupContactsInfoArray
					.get(contactPosition
							- _mTalkingGroupContactsPhoneArray.size());
		}

		// update contact is selected flag
		_selectedContact.getExtension().put(CONTACT_IS_SELECTED, false);

		// update address book contacts listView, if the selected contact is
		// present in address book contacts listView
		if (_mPresentContactsInABInfoArray.contains(_selectedContact)) {
			// get in address book present contacts adapter
			InAB6In7PreinTalkingGroupContactAdapter _inABContactAdapter = (InAB6In7PreinTalkingGroupContactAdapter) _mABContactsListView
					.getAdapter();

			// get in address book present contacts adapter data map
			@SuppressWarnings("unchecked")
			Map<String, Object> _inABContactAdapterDataMap = (Map<String, Object>) _inABContactAdapter
					.getItem(_mPresentContactsInABInfoArray
							.indexOf(_selectedContact));

			// update address book present contacts adapter data map and notify
			// adapter changed
			_inABContactAdapterDataMap.put(CONTACT_IS_SELECTED,
					_selectedContact.getExtension().get(CONTACT_IS_SELECTED));
			_inABContactAdapter.notifyDataSetChanged();
		}

		// get select contact in prein talking contacts detail info list
		// position
		int _index = _mPreinTalkingGroupContactsInfoArray
				.indexOf(_selectedContact);

		// remove from in and prein talking group contacts adapter data list and
		// notify adapter changed
		_mPreinTalkingGroupContactsInfoArray.remove(_index);
		_mIn7PreinTalkingGroupContactsAdapterDataList
				.remove(_mTalkingGroupContactsPhoneArray.size() + _index);
		((InAB6In7PreinTalkingGroupContactAdapter) _mIn7PreinTalkingGroupContactsListView
				.getAdapter()).notifyDataSetChanged();
	}

	// reset selected contacts selected flag
	private void resetSelectedContacts() {
		for (ContactBean _selectedContact : _mPreinTalkingGroupContactsInfoArray) {
			_selectedContact.getExtension().put(CONTACT_IS_SELECTED, false);
		}
	}

	private void dismissProgressDlg() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	private boolean checkPhoneFormat(String phone){
		return phone.matches("(^[0]\\d{2,3}\\d{7,8})|(^[1][\\d]{10})");
	}
	
	private void checkPhoneFormatAndChange(String phone,int position){
		if(checkPhoneFormat(phone)){
			Log.d("debug : format right ", "formatRight");
			markContactSelected(phone, position, true);
		}
		else{
			//if format is wrong, a popwindow will show up
			Log.d("debug : format wrong ", "formatWrong");
			_mWrongPhoneFormatInformPopupWindow.setInformation(position,phone);
			_mWrongPhoneFormatInformPopupWindow.showAtLocation(_mABContactsListView, Gravity.CENTER, 0, 0);
		}
	}

	@Override
	public void onBackPressed() {
		// reset selected contacts selected flag
		resetSelectedContacts();
		//remove the handler for update UI
		//AddressBookManager.getInstance().removeContactObserverHandler();
		ContactSyncService.removeHandler();
		super.onBackPressed();
	}

	// inner class
	// talking group status
	public static enum TalkingGroupStatus {
		ESTABLISHING, GOING
	}

	// contact search status
	enum ContactSearchStatus {
		NONESEARCH, SEARCHBYNAME, SEARCHBYCHINESENAME, SEARCHBYPHONE
	}

	// nav back button on click listener
	class NavBackBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// back to last activity
			resetSelectedContacts();
			setResult(RESULT_CANCELED);
			popActivity();
		}

	}

	private JSONArray getSelectedMembers() {
		String accountName = UserManager.getInstance().getUser().getName();
		JSONArray attendeesToInvite = new JSONArray();
		for (ContactBean contact : _mPreinTalkingGroupContactsInfoArray) {
			Map<String, Object> ext = contact.getExtension();
			String phoneNumber = (String) ext
					.get(SELECTED_CONTACT_SELECTEDPHONE);
			if (!accountName.equals(phoneNumber)) {
				attendeesToInvite.put(phoneNumber);
			}
		}
		return attendeesToInvite;
	}

	// open talking group button on click listener
	class OpenTalkingGroupBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// create group talking
			Log.d(SystemConstants.TAG, "create group talking");
			progressDialog = ProgressDialog.show(ContactSelectActivity.this,
					null, getString(R.string.sending_request));
			JSONArray attendeesToInvite = getSelectedMembers();

			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TalkGroup.attendees.name(), attendeesToInvite.toString());
			HttpUtils.postSignatureRequest(getString(R.string.server_url)
					+ getString(R.string.create_conf_url),
					PostRequestFormat.URLENCODED, params, null,
					HttpRequestType.ASYNCHRONOUS, onFinishedCreateGroupTalk);
		}

		private OnHttpRequestListener onFinishedCreateGroupTalk = new OnHttpRequestListener() {

			@Override
			public void onFinished(HttpResponseResult responseResult) {
				dismissProgressDlg();
				try {
					JSONObject data = new JSONObject(
							responseResult.getResponseText());
					String groupId = data.getString(TalkGroup.conferenceId
							.name());
					String owner = data.getString(TalkGroup.owner.name());

					Intent intent = new Intent(ContactSelectActivity.this,
							TalkingGroupActivity.class);
					intent.putExtra(TalkGroup.conferenceId.name(), groupId);
					intent.putExtra(TalkGroup.owner.name(), owner);
					resetSelectedContacts();
					startActivity(intent);
					ContactSelectActivity.this.finish();
				} catch (Exception e) {
					e.printStackTrace();
					MyToast.show(ContactSelectActivity.this,
							R.string.error_in_create_group, Toast.LENGTH_SHORT);
				}
			}

			@Override
			public void onFailed(HttpResponseResult responseResult) {
				dismissProgressDlg();
				switch (responseResult.getStatusCode()) {
				case 402:
					MyToast.show(ContactSelectActivity.this,
							R.string.payment_required, Toast.LENGTH_SHORT);
					break;

				default:
					MyToast.show(ContactSelectActivity.this,
							R.string.error_in_create_group, Toast.LENGTH_SHORT);
					break;
				}
			}
		};
	}

	// confirm add new contacts to talking group button on click listener
	class ConfirmAddNewContacts2TalkingGroupBtnOnClickListener implements
			OnClickListener {

		@Override
		public void onClick(View v) {
			// back to talking group detail info activity
			Log.d(LOG_TAG,
					"Confirm add new contacts to talking group, then back to talking group detail info activity");
			JSONArray attendeesToInvite = getSelectedMembers();
			if (attendeesToInvite.length() <= 0) {
				MyToast.show(ContactSelectActivity.this,
						R.string.no_new_member_selected, Toast.LENGTH_SHORT);
				return;
			}

			progressDialog = ProgressDialog.show(ContactSelectActivity.this,
					null, getString(R.string.sending_request));
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TalkGroup.conferenceId.name(), _mTalkingGroupId);
			params.put(TalkGroup.attendees.name(), attendeesToInvite.toString());
			HttpUtils.postSignatureRequest(getString(R.string.server_url)
					+ getString(R.string.invite_new_member_url),
					PostRequestFormat.URLENCODED, params, null,
					HttpRequestType.ASYNCHRONOUS, onFinishedInviteMember);

		}

		private OnHttpRequestListener onFinishedInviteMember = new OnHttpRequestListener() {

			@Override
			public void onFinished(HttpResponseResult responseResult) {
				dismissProgressDlg();
				resetSelectedContacts();
				setResult(RESULT_OK);
				ContactSelectActivity.this.finish();
			}

			@Override
			public void onFailed(HttpResponseResult responseResult) {
				dismissProgressDlg();
				MyToast.show(ContactSelectActivity.this,
						R.string.error_in_invite_member, Toast.LENGTH_SHORT);
			}
		};

	}

	// contact search editText text watcher
	class ContactSearchEditTextTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			// set contact search status
			if (null == s || 0 == s.length()) {
				_mContactSearchStatus = ContactSearchStatus.NONESEARCH;
			} else if (s.toString().matches("^[0-9]*$")) {
				_mContactSearchStatus = ContactSearchStatus.SEARCHBYPHONE;
			} else if (s.toString().matches(".*[\u4e00-\u9fa5].*")) {
				_mContactSearchStatus = ContactSearchStatus.SEARCHBYCHINESENAME;
			} else {
				_mContactSearchStatus = ContactSearchStatus.SEARCHBYNAME;
			}

			// update present contacts in address book detail info list
			switch (_mContactSearchStatus) {
			case SEARCHBYNAME:
				_mPresentContactsInABInfoArray = AddressBookManager
						.getInstance().getContactsByName(s.toString());
				break;

			case SEARCHBYCHINESENAME:
				_mPresentContactsInABInfoArray = AddressBookManager
						.getInstance().getContactsByChineseName(s.toString());
				break;

			case SEARCHBYPHONE:
				_mPresentContactsInABInfoArray = AddressBookManager
						.getInstance().getContactsByPhone(s.toString());
				break;

			case NONESEARCH:
			default:
				_mPresentContactsInABInfoArray = allNamePhoneticSortedContactsInfoArray;
				break;
			}

			// update contacts in address book listView adapter
			_mABContactsListView
					.setAdapter(generateInABContactAdapter(_mPresentContactsInABInfoArray));
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

	}
	
	class WrongPhoneFormatInformPopupWindow extends CommonPopupWindow{
		private int position = 0;
		private String originPhone;
		public WrongPhoneFormatInformPopupWindow(int resource,int width,int height, boolean focusable, boolean isBindDefListener){
			super(resource,width,height,focusable,isBindDefListener);
		}
		
		public WrongPhoneFormatInformPopupWindow(int resource,int width,int height){
			super(resource,width,height);
		}
		
		
		@Override
		protected void bindPopupWindowComponentsListener() {
			// TODO Auto-generated method stub
			((Button)getContentView().findViewById(R.id.wrong_phoneFormat_popupWindow_confirmBtn))
				.setOnClickListener(new WrongPhoneFormatConfirmBtnOnClickListener());
			((Button)getContentView().findViewById(R.id.wrong_phoneFormat_popupWindow_cancelBtn))
				.setOnClickListener(new WrongPhoneFormatCancelBtnOnClickListener());
		}

		@Override
		protected void resetPopupWindow() {
			// TODO Auto-generated method stub
			
		}
		
		public void setInformation(int p,String originPhone){
			position = p;
			this.originPhone = originPhone;
			((TextView)getContentView().findViewById(R.id.wrong_phoneFormat_popupWindow_titleTextView)).setText(
					AppLaunchActivity.getAppContext().getResources().getString(R.string.wrong_phone_format_inform_titleTextView_text)
					.replace("***", this.originPhone));
		}
		
		class WrongPhoneFormatConfirmBtnOnClickListener implements OnClickListener{

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dismiss();
				_mAddAreaCodePopupWindow.setInformationForChange(position,originPhone);
				_mAddAreaCodePopupWindow.showAtLocation(_mABContactsListView, Gravity.CENTER, 0, 0);
			}
			
		}
		
		class WrongPhoneFormatCancelBtnOnClickListener implements OnClickListener{

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dismiss();
			}
			
		}
		
	}
	
	class AddAreaCodePopupWindow extends CommonPopupWindow{
		private int position = 0;
		private String originPhone;
		
		public AddAreaCodePopupWindow(int resource,int width,int height, boolean focusable, boolean isBindDefListener){
			super(resource,width,height,focusable,isBindDefListener);
		}
		
		public AddAreaCodePopupWindow(int resource,int width,int height){
			super(resource,width,height);
		}

		@Override
		protected void bindPopupWindowComponentsListener() {
			// TODO Auto-generated method stub
			((Button)getContentView().findViewById(R.id.add_phoneAreaCode_popupWindow_dismiss_btn)).setOnClickListener(
					new AddAreaCodePopupWindowDismissBtnOnClickListener());
			((Button)getContentView().findViewById(R.id.add_phoneAreaCode_popupWindow_confirmBtn)).setOnClickListener(
					new AddAreaCodePopupWindowConfirmBtnOnClickListener());
		}

		@Override
		protected void resetPopupWindow() {
			// TODO Auto-generated method stub
			((EditText)getContentView().findViewById(R.id.add_phoneAreaCode_popupWindow_editText))
				.setText("");
		}
		
		class AddAreaCodePopupWindowDismissBtnOnClickListener implements OnClickListener{

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dismiss();
			}
			
		}
		
		class AddAreaCodePopupWindowConfirmBtnOnClickListener implements OnClickListener{

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String newPhone = ((EditText)getContentView().findViewById(
						R.id.add_phoneAreaCode_popupWindow_editText)).getText().toString();
				if(checkPhoneFormat(newPhone)){
					dismiss();
					//Log.d("new phone number!!", newPhone);
					//get update ID
					ContactBean selectedContact = _mPresentContactsInABInfoArray.get(position);
					long contactId = selectedContact.getId();	
					markContactSelected(newPhone,position,true); 
					//update to db
					( AddressBookManager.getInstance()).updateContactPhone(contactId,originPhone,newPhone); 
				}
				else{
					MyToast.show(
							ContactSelectActivity.this,
							ContactSelectActivity.this
											.getResources()
											.getString(
													R.string.wrong_phonenumber_format),
							Toast.LENGTH_SHORT);
				}	
			}
			
		}
		
		public void setInformationForChange(int p,String originPhone){
			position = p;
			this.originPhone = originPhone;
			((EditText)getContentView().findViewById(
					R.id.add_phoneAreaCode_popupWindow_editText)).setText(originPhone);
		}
		
	}

	// add manual input contact popup window
	class AddManualInputContactPopupWindow extends CommonPopupWindow {

		public AddManualInputContactPopupWindow(int resource, int width,
				int height, boolean focusable, boolean isBindDefListener) {
			super(resource, width, height, focusable, isBindDefListener);
		}

		public AddManualInputContactPopupWindow(int resource, int width,
				int height) {
			super(resource, width, height);
		}

		@Override
		protected void bindPopupWindowComponentsListener() {
			// bind add manual input contact popup window dismiss and confirm
			// added button on click listener
			((Button) getContentView().findViewById(
					R.id.add_manualInputContact_popupWindow_dismiss_btn))
					.setOnClickListener(new AddManualInputContactPopupWindowDismissBtnOnClickListener());
			((Button) getContentView().findViewById(
					R.id.add_manualInputContact_confirmBtn))
					.setOnClickListener(new AddManualInputContactPopupWindowConfirmAddedBtnOnClickListener());
		}

		@Override
		protected void resetPopupWindow() {
			// clear add not manual input contact editText text
			((EditText) getContentView().findViewById(
					R.id.add_manualInputContact_editText)).setText("");
		}

		// inner class
		// add manual input contact popup window dismiss button on click
		// listener
		class AddManualInputContactPopupWindowDismissBtnOnClickListener
				implements OnClickListener {

			@Override
			public void onClick(View v) {
				// dismiss add manual input contact popup window
				dismiss();
			}

		}

		// add manual input contact popup window confirm added button on click
		// listener
		class AddManualInputContactPopupWindowConfirmAddedBtnOnClickListener
				implements OnClickListener {

			@Override
			public void onClick(View v) {
				// get added manual input contact phone number
				String _addedManualInputContactPhoneNumber = ((EditText) getContentView()
						.findViewById(R.id.add_manualInputContact_editText))
						.getText().toString();
				
				// check added manual input contact phone number
				if (null == _addedManualInputContactPhoneNumber
						|| _addedManualInputContactPhoneNumber
								.equalsIgnoreCase("")) {
					MyToast.show(ContactSelectActivity.this,
							R.string.toast_manualInputContact_phoneNumber_null,
							Toast.LENGTH_SHORT);

					return;
				}

				if (_addedManualInputContactPhoneNumber
						.endsWith(getString(R.string.call_center))) {
					MyToast.show(
							ContactSelectActivity.this,
							R.string.toast_phone_number_identical_to_call_center,
							Toast.LENGTH_SHORT);
					return;
				}
				
				if(!checkPhoneFormat(_addedManualInputContactPhoneNumber)){
					MyToast.show(
							ContactSelectActivity.this,
							ContactSelectActivity.this
											.getResources()
											.getString(
													R.string.wrong_phonenumber_format),
							Toast.LENGTH_SHORT);
				}
				
				else{
					// dismiss add manual input contact popup window
					dismiss();
	
					// get address book manager
					AddressBookManager _addressBookManager = AddressBookManager
							.getInstance();
	
					// check the added manual input contact with phone number is in
					// address book
					Long _manualInputContactId = _addressBookManager
							.isContactWithPhoneInAddressBook(_addedManualInputContactPhoneNumber);
					if (null == _manualInputContactId) {
						// check the new added contact is in talking group attendees
						if (_mTalkingGroupContactsPhoneArray
								.contains(_addedManualInputContactPhoneNumber)) {
							MyToast.show(
									ContactSelectActivity.this,
									AddressBookManager
											.getInstance()
											.getContactsDisplayNamesByPhone(
													_addedManualInputContactPhoneNumber)
											.get(0)
											+ ContactSelectActivity.this
													.getResources()
													.getString(
															R.string.toast_selectedContact_existedInTalkingGroup_attendees),
									Toast.LENGTH_SHORT);
	
							return;
						}
						// check the new added contact is in prein talking group
						// contacts
						for (ContactBean _preinTalkingGroupContact : _mPreinTalkingGroupContactsInfoArray) {
							if (_addedManualInputContactPhoneNumber
									.equalsIgnoreCase((String) _preinTalkingGroupContact
											.getExtension().get(
													SELECTED_CONTACT_SELECTEDPHONE))) {
								MyToast.show(
										ContactSelectActivity.this,
										_preinTalkingGroupContact.getDisplayName()
												+ ContactSelectActivity.this
														.getResources()
														.getString(
																R.string.toast_selectedContact_useTheSelectedPhone_existedInPreinTalkingGroup_contacts),
										Toast.LENGTH_SHORT);
	
								return;
							}
						}
	
						// generate new added contact
						ContactBean _newAddedContact = new ContactBean();
	
						// init new added contact
						// set aggregated id
						_newAddedContact.setId(-1L);
						// set display name
						_newAddedContact
								.setDisplayName(_addedManualInputContactPhoneNumber);
						// set phone numbers
						List<String> _phoneNumbersList = new ArrayList<String>();
						_phoneNumbersList.add(_addedManualInputContactPhoneNumber);
						_newAddedContact.setPhoneNumbers(_phoneNumbersList);
						// set selected contact the selected phone
						_newAddedContact.getExtension().put(
								SELECTED_CONTACT_SELECTEDPHONE,
								_addedManualInputContactPhoneNumber);
						// set contact is selected flag
						_newAddedContact.getExtension().put(CONTACT_IS_SELECTED,
								true);
	
						// add new added contact to in and prein talking group
						// contacts adapter data list and notify adapter changed
						_mPreinTalkingGroupContactsInfoArray.add(_newAddedContact);
						_mIn7PreinTalkingGroupContactsAdapterDataList
								.add(generateIn6PreinTalkingGroupAdapterData(
										_addedManualInputContactPhoneNumber, false));
						((InAB6In7PreinTalkingGroupContactAdapter) _mIn7PreinTalkingGroupContactsListView
								.getAdapter()).notifyDataSetChanged();
					} else {
						// get the matched contact
						ContactBean _matchedContact = _addressBookManager
								.getContactByAggregatedId(_manualInputContactId);
	
						// check the matched contact is selected flag
						if ((Boolean) _matchedContact.getExtension().get(
								CONTACT_IS_SELECTED)) {
							MyToast.show(
									ContactSelectActivity.this,
									_matchedContact.getDisplayName()
											+ ContactSelectActivity.this
													.getResources()
													.getString(
															_addedManualInputContactPhoneNumber
																	.equalsIgnoreCase((String) _matchedContact
																			.getExtension()
																			.get(SELECTED_CONTACT_SELECTEDPHONE)) ? R.string.toast_selectedContact_useTheSelectedPhone_existedInPreinTalkingGroup_contacts
																	: R.string.toast_selectedContact_useAnotherPhone_existedInPreinTalkingGroup_contacts),
									Toast.LENGTH_SHORT);
	
							return;
						}
	
						// check the matched contact in address book listView
						// present contacts list
						if (_mPresentContactsInABInfoArray
								.contains(_matchedContact)) {
							// mark contact selected
							markContactSelected(
									_addedManualInputContactPhoneNumber,
									_mPresentContactsInABInfoArray
											.indexOf(_matchedContact), true);
						} else {
							// mark contact selected
							markContactSelected(
									_addedManualInputContactPhoneNumber,
									allNamePhoneticSortedContactsInfoArray
											.indexOf(_matchedContact), false);
						}
					}
				}
			}

		}

	}

	// add manual input contact button on click listener
	class AddManualInputContactBtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// show add manual input contact popup window
			_mAddManualInputContactPopupWindow.showAtLocation(v,
					Gravity.CENTER, 0, 0);
		}

	}
	
	
	class UpdateABListHandler extends Handler{
		
		public UpdateABListHandler(){
			super();
		}
		
		 public void handleMessage(Message msg) {			 
			 int type = msg.what;
//			 Log.d("Contact", "getMessage : " + type);
			 
			 if(type==1){
				 //contacts have been create or delete
				 AddressBookManager.getInstance().copyAllContactsInfo(allNamePhoneticSortedContactsInfoArray);
			 }
			 
			 String searchString = ((EditText)ContactSelectActivity.this.findViewById(R.id.contact_search_editText)).getText().toString();
//			 Log.d("ContactSelectActivity", "searchString ：" + searchString);
//			 Log.d("ContactSelectActivity", "searchStatus ：" + _mContactSearchStatus);
			 switch (_mContactSearchStatus) {
				case SEARCHBYNAME:
					_mPresentContactsInABInfoArray = AddressBookManager
							.getInstance().getContactsByName(searchString.toString());
					break;

				case SEARCHBYCHINESENAME:
					_mPresentContactsInABInfoArray = AddressBookManager
							.getInstance().getContactsByChineseName(searchString.toString());
					break;

				case SEARCHBYPHONE:
					_mPresentContactsInABInfoArray = AddressBookManager
							.getInstance().getContactsByPhone(searchString.toString());
					break;

				case NONESEARCH:
				default:
					_mPresentContactsInABInfoArray = allNamePhoneticSortedContactsInfoArray;
					break;
				}
			// update contacts in address book listView adapter
			 //Log.d("ContactSelectActivity", "search result : " + _mPresentContactsInABInfoArray.size());
			_mABContactsListView
					.setAdapter(generateInABContactAdapter(_mPresentContactsInABInfoArray));
			 
			 /*if(type==1){
				 String searchString = ((EditText)ContactSelectActivity.this.findViewById(R.id.contact_search_editText)).getText().toString();
//				 Log.d("ContactSelectActivity", "searchString ：" + searchString);
//				 Log.d("ContactSelectActivity", "searchStatus ：" + _mContactSearchStatus);
				 switch (_mContactSearchStatus) {
					case SEARCHBYNAME:
						_mPresentContactsInABInfoArray = AddressBookManager
								.getInstance().getContactsByName(searchString.toString());
						break;
	
					case SEARCHBYCHINESENAME:
						_mPresentContactsInABInfoArray = AddressBookManager
								.getInstance().getContactsByChineseName(searchString.toString());
						break;
	
					case SEARCHBYPHONE:
						_mPresentContactsInABInfoArray = AddressBookManager
								.getInstance().getContactsByPhone(searchString.toString());
						break;
	
					case NONESEARCH:
					default:
						_mPresentContactsInABInfoArray = allNamePhoneticSortedContactsInfoArray;
						break;
					}
				// update contacts in address book listView adapter
				 Log.d("ContactSelectActivity", "search result : " + _mPresentContactsInABInfoArray.size());
				_mABContactsListView
						.setAdapter(generateInABContactAdapter(_mPresentContactsInABInfoArray));
			 }
			 else if(type ==2){
				 //delelte the contact 
				 ContactBean deleteContact = (ContactBean) msg.obj;
				 allNamePhoneticSortedContactsInfoArray.remove(deleteContact);
			 }
			 else if(type==3){
				 AddressBookManager.getInstance().copyAllContactsInfo(allNamePhoneticSortedContactsInfoArray);
			 }*/
		 }
	}

	// contact phone numbers select popup window
	class ContactPhoneNumbersSelectPopupWindow extends CommonPopupWindow {

		// select contact position
		private int _mSelectContactPosition;

		public ContactPhoneNumbersSelectPopupWindow(int resource, int width,
				int height, boolean focusable, boolean isBindDefListener) {
			super(resource, width, height, focusable, isBindDefListener);
		}

		public ContactPhoneNumbersSelectPopupWindow(int resource, int width,
				int height) {
			super(resource, width, height);
		}

		@Override
		protected void bindPopupWindowComponentsListener() {

			// get contact phones select phone button parent linearLayout
			LinearLayout _phoneBtnParentLinearLayout = (LinearLayout) getContentView()
					.findViewById(
							R.id.contactPhones_select_phoneBtn_linearLayout);

			// bind contact phone select phone button click listener
			for (int i = 0; i < _phoneBtnParentLinearLayout.getChildCount(); i++) {
				((Button) _phoneBtnParentLinearLayout.getChildAt(i))
						.setOnClickListener(new ContactPhoneSelectPhoneBtnOnClickListener());
			}

			// bind contact phone select phone listView item click listener
			((ListView) getContentView().findViewById(
					R.id.contactPhones_select_phonesListView))
					.setOnItemClickListener(new ContactPhoneSelectPhoneListViewOnItemClickListener());

			// bind contact phone select cancel button click listener
			((Button) getContentView().findViewById(
					R.id.contactPhones_select_cancelBtn))
					.setOnClickListener(new ContactPhoneSelectCancelBtnOnClickListener());
		}

		@Override
		protected void resetPopupWindow() {
			// hide contact phones select phone list view
			((ListView) getContentView().findViewById(
					R.id.contactPhones_select_phonesListView))
					.setVisibility(View.GONE);

			// get contact phones select phone button parent linearLayout and
			// hide it
			LinearLayout _phoneBtnParentLinearLayout = (LinearLayout) getContentView()
					.findViewById(
							R.id.contactPhones_select_phoneBtn_linearLayout);
			_phoneBtnParentLinearLayout.setVisibility(View.GONE);

			// process phone button
			for (int i = 0; i < _phoneBtnParentLinearLayout.getChildCount(); i++) {
				// hide contact phones select phone button
				((Button) _phoneBtnParentLinearLayout.getChildAt(i))
						.setVisibility(View.GONE);
			}
		}

		// set contact phone number for selecting
		public void setContactPhones4Selecting(String displayName,
				List<String> phoneNumbers, int position) {
			// update select contact position
			_mSelectContactPosition = position;

			// set contact phones select title textView text
			((TextView) getContentView().findViewById(
					R.id.contactPhones_select_titleTextView))
					.setText(AppLaunchActivity
							.getAppContext()
							.getResources()
							.getString(
									R.string.contactPhones_selectPopupWindow_titleTextView_text)
							.replace("***", displayName));

			// check phone numbers for selecting
			if (2 <= phoneNumbers.size() && phoneNumbers.size() <= 3) {
				// get contact phones select phone button parent linearLayout
				// and show it
				LinearLayout _phoneBtnParentLinearLayout = (LinearLayout) getContentView()
						.findViewById(
								R.id.contactPhones_select_phoneBtn_linearLayout);
				_phoneBtnParentLinearLayout.setVisibility(View.VISIBLE);

				// process phone button
				for (int i = 0; i < phoneNumbers.size(); i++) {
					// get contact phones select phone button
					Button _phoneBtn = (Button) _phoneBtnParentLinearLayout
							.getChildAt(i);

					// set button text and show it
					_phoneBtn.setText(phoneNumbers.get(i));
					_phoneBtn.setVisibility(View.VISIBLE);
				}
			} else {
				// get contact phones select phone list view
				ListView _phoneListView = (ListView) getContentView()
						.findViewById(R.id.contactPhones_select_phonesListView);

				// set phone list view adapter
				_phoneListView
						.setAdapter(new ArrayAdapter<String>(
								AppLaunchActivity.getAppContext(),
								R.layout.contact_phonenumbers_select_phoneslist_item_layout,
								phoneNumbers));

				// show phone list view
				_phoneListView.setVisibility(View.VISIBLE);
			}
		}

		// inner class
		// contact phone select phone button on click listener
		class ContactPhoneSelectPhoneBtnOnClickListener implements
				OnClickListener {

			@Override
			public void onClick(View v) {
				// get phone button text
				String _selectedPhone = (String) ((Button) v).getText();

				// dismiss contact phone select popup window
				dismiss();

				//check phone format, if format is not right then change and  mark contact selected
				checkPhoneFormatAndChange(_selectedPhone,_mSelectContactPosition);
			}

		}

		// contact phone select phone listView on item click listener
		class ContactPhoneSelectPhoneListViewOnItemClickListener implements
				OnItemClickListener {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// get phone listView item data
				String _selectedPhone = (String) ((TextView) view).getText();

				// dismiss contact phone select popup window
				dismiss();

				////check phone format, if format is not right then change and  mark contact selected
				checkPhoneFormatAndChange(_selectedPhone,_mSelectContactPosition);
			}

		}

		// contact phone select cancel button on click listener
		class ContactPhoneSelectCancelBtnOnClickListener implements
				OnClickListener {

			@Override
			public void onClick(View v) {
				// dismiss contact phone select popup window
				dismiss();
			}

		}

	}

	// contacts in address book listView on item click listener
	class ContactsInABListViewOnItemClickListener implements
			OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// get the click item view data: contact object
			ContactBean _clickItemViewData = _mPresentContactsInABInfoArray
					.get((int) id);

			// get contact is selected flag
			Boolean _isSelected = (Boolean) _clickItemViewData.getExtension()
					.get(CONTACT_IS_SELECTED);

			// check the click item view data(contact object) is selected
			if (_isSelected) {
				// mark contact unselected
				markContactUnselected((int) id, true);
			} else {
				// check the click item view data
				if (null == _clickItemViewData.getPhoneNumbers()) {
					// show contact has no phone number alert dialog
					new AlertDialog.Builder(ContactSelectActivity.this)
							.setTitle(
									R.string.contact_hasNoPhone_alertDialog_title)
							.setMessage(_clickItemViewData.getDisplayName())
							.setPositiveButton(
									R.string.contact_hasNoPhone_alertDialog_reselectBtn_title,
									null).show();
				} else {
					switch (_clickItemViewData.getPhoneNumbers().size()) {
					case 1:
						//check phone format and  mark contact selected
						checkPhoneFormatAndChange(_clickItemViewData.getPhoneNumbers().get(0),position);					
						break;

					default:
						// hide soft keyboard
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(
								((EditText) findViewById(R.id.contact_search_editText))
										.getWindowToken(), 0);

						// set contact phone numbers for selecting
						_mContactPhoneNumbersSelectPopupWindow
								.setContactPhones4Selecting(
										_clickItemViewData.getDisplayName(),
										_clickItemViewData.getPhoneNumbers(),
										position);

						// show contact phone numbers select popup window
						_mContactPhoneNumbersSelectPopupWindow.showAtLocation(
								parent, Gravity.CENTER, 0, 0);

						break;
					}
				}
			}
		}

	}

	// contacts in address book listView quick alphabet bar on touch listener
	class ContactsInABListViewQuickAlphabetBarOnTouchListener extends
			OnTouchListener {

		@Override
		protected boolean onTouch(RelativeLayout alphabetRelativeLayout,
				ListView dependentListView, MotionEvent event,
				Character alphabeticalCharacter) {
			// get scroll position
			if (dependentListView.getAdapter() instanceof CommonListAdapter) {
				// get dependent listView adapter
				CommonListAdapter _commonListAdapter = (CommonListAdapter) dependentListView
						.getAdapter();

				for (int i = 0; i < _commonListAdapter.getCount(); i++) {
					// get alphabet index
					@SuppressWarnings("unchecked")
					String _alphabetIndex = (String) ((Map<String, ?>) _commonListAdapter
							.getItem(i)).get(CommonListAdapter.ALPHABET_INDEX);

					// check alphabet index
					if (null == _alphabetIndex
							|| _alphabetIndex.startsWith(String.valueOf(
									alphabeticalCharacter).toLowerCase())) {
						// set selection
						dependentListView.setSelection(i);

						break;
					}
				}
			} else {
				Log.e(LOG_TAG, "Dependent listView adapter = "
						+ dependentListView.getAdapter() + " and class name = "
						+ dependentListView.getAdapter().getClass().getName());
			}

			return true;
		}

	}

	// contacts in and prein talking group listView on item click listener
	class ContactsIn7PreinTalkingGroupListViewOnItemClickListener implements
			OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// check clicked item position
			if (position >= _mTalkingGroupContactsPhoneArray.size()) {
				// mark contact unselected
				markContactUnselected((int) id, false);
			}
		}

	}
}
