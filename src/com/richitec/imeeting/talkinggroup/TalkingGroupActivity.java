package com.richitec.imeeting.talkinggroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.richitec.commontoolkit.user.UserManager;
import com.richitec.commontoolkit.utils.HttpUtils;
import com.richitec.commontoolkit.utils.HttpUtils.HttpRequestType;
import com.richitec.commontoolkit.utils.HttpUtils.HttpResponseResult;
import com.richitec.commontoolkit.utils.HttpUtils.OnHttpRequestListener;
import com.richitec.commontoolkit.utils.HttpUtils.PostRequestFormat;
import com.richitec.commontoolkit.utils.MyToast;
import com.richitec.imeeting.R;
import com.richitec.imeeting.constants.Attendee;
import com.richitec.imeeting.constants.Notify;
import com.richitec.imeeting.constants.SystemConstants;
import com.richitec.imeeting.constants.TalkGroup;
import com.richitec.imeeting.contactselect.ContactSelectActivity;
import com.richitec.imeeting.contactselect.ContactSelectActivity.TalkingGroupStatus;
import com.richitec.imeeting.talkinggroup.adapter.MemberListAdapter;
import com.richitec.imeeting.talkinggroup.statusfilter.AttendeeModeStatusFilter;
import com.richitec.imeeting.talkinggroup.statusfilter.OwnerModeStatusFilter;
import com.richitec.imeeting.util.AppUtil;
import com.richitec.imeeting.video.VideoManager;
import com.richitec.websocket.notifier.NotifierCallbackListener;
import com.richitec.websocket.notifier.WebSocketNotifier;

public class TalkingGroupActivity extends Activity implements OnGestureListener {
	public static final String TALKINGGROUP_ACTIVITY_PARAM_TALKINGGROUPID = "talking group id";
	public static final String TALKINGGROUP_ACTIVITY_PARAM_TALKINGGROUP_ATTENDEESPHONE = "talking group attendees phone";

	private static final int REQ_CONTACT_SELECT = 0;
	private static final int MIN_FLING_DISTANCE = 100;

	private ProgressDialog progressDlg;
	private MemberListAdapter memberListAdatper;
	private Handler handler;
	private PullToRefreshListView memberListView;
	private ViewFlipper flipper;
	private GestureDetector gestureDetector;

	private VideoManager videoManager;

	private WebSocketNotifier notifier;

	private String groupId;
	private String owner;

	private Map<String, String> selectedMember;
	private Timer timer;

	private FrameLayout largeVideoLayout;
	private FrameLayout smallVideoLayout;

	private WakeLock wakeLock;

	enum GTViewType {
		MemberListView, VideoView
	}

	private GTViewType currentView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_talking_group);
		handler = new Handler(Looper.myLooper());

		Intent intent = getIntent();
		groupId = intent.getStringExtra(TalkGroup.conferenceId.name());
		owner = intent.getStringExtra(TalkGroup.owner.name());

		gestureDetector = new GestureDetector(this);
		flipper = (ViewFlipper) findViewById(R.id.gt_view_flipper);
		currentView = GTViewType.MemberListView;

		videoManager = new VideoManager(this);
		videoManager.setGroupId(groupId);
		videoManager.setLiveName(UserManager.getInstance().getUser().getName());
		videoManager.setRtmpUrl(getString(R.string.rtmp_server_url));
		videoManager.setImgWidth(144);
		videoManager.setImgHeight(192);

		PowerManager powerMan = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerMan.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		
		initUI();

		notifier = new WebSocketNotifier();
		notifier.setServerAddress(getString(R.string.notify_url));
		notifier.setSubscriberID(UserManager.getInstance().getUser().getName());
		notifier.setTopic(groupId);
		notifier.setNotifierActionListener(notifyCallbackListener);
		notifier.connect();

		timer = new Timer();
		timer.schedule(new HeartBeatTimerTask(), 10000, 10000);
	}

	private void initUI() {
		TextView titleTV = (TextView) findViewById(R.id.gt_title);
		String title = getString(R.string.group_talk_title) + groupId;
		titleTV.setText(title);

		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();

		int btWidth = isOwner() ? width / 4 : width / 2;

		Button addMemberBt = (Button) findViewById(R.id.gt_add_member_bt);
		Button smsInviteBt = (Button) findViewById(R.id.gt_sms_invite_bt);
		Button dialBt = (Button) findViewById(R.id.gt_dial_bt);
		Button leaveBt = (Button) findViewById(R.id.gt_leave_bt);

		if (isOwner()) {
			LayoutParams params = addMemberBt.getLayoutParams();
			params.width = btWidth - 1;
			addMemberBt.setLayoutParams(params);

			params = smsInviteBt.getLayoutParams();
			params.width = btWidth - 2;
			smsInviteBt.setLayoutParams(params);
		} else {
			addMemberBt.setVisibility(View.GONE);
			smsInviteBt.setVisibility(View.GONE);
			View sep1 = findViewById(R.id.gt_bottom_sep1);
			View sep2 = findViewById(R.id.gt_bottom_sep2);
			sep1.setVisibility(View.GONE);
			sep2.setVisibility(View.GONE);
		}

		LayoutParams params = dialBt.getLayoutParams();
		params.width = isOwner() ? btWidth - 2 : btWidth - 1;
		dialBt.setLayoutParams(params);

		dialBt.setText(isOwner() ? R.string.dial_all : R.string.dial);

		params = leaveBt.getLayoutParams();
		params.width = btWidth - 1;
		leaveBt.setLayoutParams(params);

		// init list
		memberListView = (PullToRefreshListView) findViewById(R.id.gt_memberlist);
		memberListAdatper = new MemberListAdapter(this);

		memberListView.getRefreshableView().setAdapter(memberListAdatper);
		memberListView.getRefreshableView().setOnItemClickListener(
				onMemberSeletecedListener);

		memberListView.setOnRefreshListener(new OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				refreshMemberList();
			}
		});

		if (isOwner()) {
			memberListAdatper.setStatusFilter(new OwnerModeStatusFilter());
		} else {
			memberListAdatper.setStatusFilter(new AttendeeModeStatusFilter());
		}

		refreshMemberList();

		largeVideoLayout = (FrameLayout) findViewById(R.id.large_video_layout);
		smallVideoLayout = (FrameLayout) findViewById(R.id.small_video_layout);
	}

	private boolean isOwner() {
		String accountName = UserManager.getInstance().getUser().getName();
		boolean isOwner = accountName.equals(owner) ? true : false;
		return isOwner;
	}

	public void onSwitchToVideoView(View v) {
		if (currentView == GTViewType.MemberListView) {
			showVideoView();
			currentView = GTViewType.VideoView;
		}
	}

	public void onSwitchToMemberListView(View v) {
		if (currentView == GTViewType.VideoView) {
			showMemberListView();
			currentView = GTViewType.MemberListView;
		}
	}

	public void onAddMemberAction(View v) {
		Intent intent = new Intent(this, ContactSelectActivity.class);
		Bundle bundle = new Bundle();
		intent.putExtra(
				ContactSelectActivity.CONTACT_SELECT_ACTIVITY_PARAM_TALKINGGROUPSTATUS,
				TalkingGroupStatus.GOING);
		bundle.putString(TALKINGGROUP_ACTIVITY_PARAM_TALKINGGROUPID, groupId);

		ArrayList<String> inAttendees = new ArrayList<String>();
		String accountName = UserManager.getInstance().getUser().getName();
		for (int i = 0; i < memberListAdatper.getCount(); i++) {
			Map<String, String> member = (Map<String, String>) memberListAdatper
					.getItem(i);
			String userName = member.get(Attendee.username.name());
			if (!accountName.equals(userName)) {
				inAttendees.add(userName);
			}
		}
		bundle.putStringArrayList(
				TALKINGGROUP_ACTIVITY_PARAM_TALKINGGROUP_ATTENDEESPHONE,
				inAttendees);
		intent.putExtras(bundle);
		startActivityForResult(intent, REQ_CONTACT_SELECT);
	}

	public void onSmsInviteAction(View v) {
		List<Map<String, String>> memberList = memberListAdatper
				.getMemberList();
		String accountName = UserManager.getInstance().getUser().getName();
		List<String> numbers = new ArrayList<String>();
		for (Map<String, String> member : memberList) {
			String memberName = member.get(Attendee.username.name());
			if (!accountName.equals(memberName)) {
				numbers.add(memberName);
			}
		}
		if (numbers.size() > 0) {
			sendSMS(numbers);
		} else {
			MyToast.show(this, R.string.no_one_to_invite, Toast.LENGTH_SHORT);
		}

	}

	public void onDialAction(View v) {

		if (isOwner()) {
			// in owner mode, call all member
			if (isNeedCallAllMember()) {
				new AlertDialog.Builder(this)
						.setTitle(R.string.alert_title)
						.setMessage(R.string.is_need_call_all_member)
						.setPositiveButton(R.string.call,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										callAllMember();
									}
								}).setNegativeButton(R.string.cancel, null)
						.show();
			} else {
				new AlertDialog.Builder(this).setTitle(R.string.alert_title)
						.setMessage(R.string.no_member_need_to_call)
						.setPositiveButton(R.string.ok, null).show();
			}
		} else {
			// in attendee mode, just call myself into conference
			Button dialButton = (Button) findViewById(R.id.gt_dial_bt);
			String text = dialButton.getText().toString();
			if (text.equals(getString(R.string.dial))) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						TalkingGroupActivity.this)
						.setMessage(
								getString(R.string.do_you_want_to_call_into_group_talking))
						.setPositiveButton(R.string.call_in,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										callMeIntoGroupTalking();
									}
								}).setNegativeButton(R.string.cancel, null);
				builder.show();
			} else if (text.equals(getString(R.string.calling_in))) {

			} else if (text.equals(getString(R.string.hangup_talking))) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						TalkingGroupActivity.this)
						.setMessage(
								getString(R.string.do_you_wanna_end_group_talking))
						.setPositiveButton(R.string.end_group_talking,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										hangMeUpFromGroupTalking();
									}
								}).setNegativeButton(R.string.cancel, null);
				builder.show();
			}
		}

	}

	/**
	 * check if need to call al member
	 * 
	 * @return true: need call, false: don't need
	 */
	private boolean isNeedCallAllMember() {
		List<Map<String, String>> memberList = memberListAdatper
				.getMemberList();
		boolean need = false;
		for (Map<String, String> member : memberList) {
			String phoneStatus = member.get(Attendee.telephone_status.name());
			if (Attendee.PhoneStatus.Failed.name().equals(phoneStatus)
					|| Attendee.PhoneStatus.Terminated.name().equals(
							phoneStatus)) {
				need = true;
				break;
			}
		}
		return need;
	}

	public void onLeaveAction(View v) {
		if (isOwner()) {
			// owner mode
			new AlertDialog.Builder(this)
					.setTitle(R.string.select_operation)
					.setItems(R.array.leave_talkgroup_menu,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									switch (which) {
									case 0:
										leaveGroupTalk();
										break;
									case 1:
										closeGroupTalk();
										break;
									default:
										break;
									}

								}
							}).show();
		} else {
			// normal attendee mode
			AlertDialog.Builder builder = new AlertDialog.Builder(
					TalkingGroupActivity.this)
					.setTitle(R.string.alert_title)
					.setMessage(getString(R.string.do_you_want_to_leave_group))
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									leaveGroupTalk();
								}
							}).setNegativeButton(R.string.cancel, null);
			builder.show();
		}
	}

	public void onCameraButtonAction(View v) {
		if (!videoManager.isVideoLiving()) {
			try {
				videoManager.startVideoLive();
				videoManager.attachVideoPreview(smallVideoLayout);
				onCameraOpen();
			} catch (Exception e) {
				MyToast.show(this, R.string.camera_cannot_open,
						Toast.LENGTH_SHORT);
				e.printStackTrace();
			}
		} else {
			videoManager.detachVideoPreview();
			videoManager.stopVideoLive();
			onCameraClosed();
		}
	}

	public void onSwitchCameraAction(View v) {
		videoManager.switchCamera();
	}

	private void onCameraOpen() {
		Button cameraSwitchBt = (Button) findViewById(R.id.gt_camera_switch_bt);
		cameraSwitchBt.setVisibility(View.VISIBLE);

		Button cameraBt = (Button) findViewById(R.id.gt_camera_op_bt);
		cameraBt.setText(R.string.close_camera);
		
		wakeLock.acquire();
	}

	private void onCameraClosed() {
		Button cameraSwitchBt = (Button) findViewById(R.id.gt_camera_switch_bt);
		cameraSwitchBt.setVisibility(View.GONE);

		Button cameraBt = (Button) findViewById(R.id.gt_camera_op_bt);
		cameraBt.setText(R.string.open_camera);
		
		wakeLock.release();
	}

	@Override
	public void onBackPressed() {
		onLeaveAction(null);
	}

	private void leaveGroupTalk() {
		timer.cancel();
		notifier.disconnect();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.unjoin_conf_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, null);
		TalkingGroupActivity.this.finish();
	}

	/**
	 * send request to server to destroy
	 */
	private void closeGroupTalk() {
		progressDlg = ProgressDialog.show(TalkingGroupActivity.this, null,
				getString(R.string.destroying_talkgroup));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.destroy_conf_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedCloseGroupTalk);
	}

	private OnHttpRequestListener onFinishedCloseGroupTalk = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			onCloseGroupTalkRequestReturn();
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			onCloseGroupTalkRequestReturn();
		}
	};

	private void onCloseGroupTalkRequestReturn() {
		dismissProgressDlg();
		timer.cancel();
		notifier.disconnect();
		TalkingGroupActivity.this.finish();
	}

	private void refreshMemberList() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.get_attendee_list_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedGetMemberList);

	}

	private OnHttpRequestListener onFinishedGetMemberList = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			memberListView.onRefreshComplete();

			try {
				JSONArray attendees = new JSONArray(
						responseResult.getResponseText());
				memberListAdatper.setData(attendees);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			memberListView.onRefreshComplete();

		}
	};

	private OnItemClickListener onMemberSeletecedListener = new OnItemClickListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectedMember = (Map<String, String>) memberListAdatper
					.getItem(position - 1);
			if (isOwner()) {
				doActionForSelectedMemberInOwnerMode(selectedMember);
			} else {
				doActionForSelectedMemberInAttendeeMode(selectedMember);
			}
		}
	};

	private void doActionForSelectedMemberInOwnerMode(Map<String, String> member) {
		final String userName = member.get(Attendee.username.name());
		String displayName = AppUtil.getDisplayNameFromAttendee(member);
		String onlineStatus = member.get(Attendee.online_status.name());
		String phoneStatus = member.get(Attendee.telephone_status.name());

		String accountName = UserManager.getInstance().getUser().getName();

		List<String> actionList = new ArrayList<String>();
		if (!Attendee.OnlineStatus.online.name().equals(onlineStatus)
				|| accountName.equals(userName)) {
			if (Attendee.PhoneStatus.Terminated.name().equals(phoneStatus)
					|| Attendee.PhoneStatus.Failed.name().equals(phoneStatus)
					|| Attendee.PhoneStatus.TermWait.name().equals(phoneStatus)) {
				actionList.add(getString(R.string.call));
			} else if (Attendee.PhoneStatus.CallWait.name().equals(phoneStatus)
					|| Attendee.PhoneStatus.Established.name().equals(
							phoneStatus)) {
				actionList.add(getString(R.string.hang_up));
			}
		}

		if (!accountName.equals(userName)) {
			actionList.add(getString(R.string.send_sms));
			actionList.add(getString(R.string.kick_out));
		}

		actionList.add(getString(R.string.cancel));

		String operationTitle = String.format(
				getString(R.string.operation_on_sb), displayName);
		final String[] actions = actionList.toArray(new String[] {});
		new AlertDialog.Builder(this).setTitle(operationTitle)
				.setItems(actions, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String action = actions[which];
						if (action.equals(getString(R.string.call))) {
							call(userName);
						} else if (action.equals(getString(R.string.hang_up))) {
							hangup(userName);
						} else if (action.equals(getString(R.string.send_sms))) {
							ArrayList<String> numbers = new ArrayList<String>();
							numbers.add(userName);
							sendSMS(numbers);
						} else if (action.equals(getString(R.string.kick_out))) {
							kickout(userName);
						}
					}
				}).show();
	}

	private void doActionForSelectedMemberInAttendeeMode(
			Map<String, String> member) {

	}

	private void call(String targetUserName) {
		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.sending_request));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("dstUserName", targetUserName);
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.call_url), PostRequestFormat.URLENCODED,
				params, null, HttpRequestType.ASYNCHRONOUS, onFinishedCall);
	}

	private OnHttpRequestListener onFinishedCall = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			String userName = selectedMember.get(Attendee.username.name());
			Map<String, String> attendee = new HashMap<String, String>();
			attendee.put(Attendee.username.name(), userName);
			attendee.put(Attendee.telephone_status.name(),
					Attendee.PhoneStatus.CallWait.name());
			memberListAdatper.updateMember(attendee);
		}

		@Override
		public void onForbidden(HttpResponseResult responseResult) {
			dismissProgressDlg();
			String userName = selectedMember.get(Attendee.username.name());
			String toastMsg = String.format(
					getString(R.string.call_is_forbidden_for_sb), userName);
			MyToast.show(TalkingGroupActivity.this, toastMsg,
					Toast.LENGTH_SHORT);
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this, R.string.call_failed,
					Toast.LENGTH_SHORT);
		}
	};

	private void hangup(String targetUserName) {
		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.sending_request));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("dstUserName", targetUserName);
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.hangup_url), PostRequestFormat.URLENCODED,
				params, null, HttpRequestType.ASYNCHRONOUS, onFinishedHangup);
	}

	private OnHttpRequestListener onFinishedHangup = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			updateHangUpUI();
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();

			int status = responseResult.getStatusCode();
			switch (status) {
			case 409:
				updateHangUpUI();
				break;
			case 403:
				String userName = selectedMember.get(Attendee.username.name());
				String toastMsg = String.format(
						getString(R.string.hangup_is_forbidden_for_sb),
						userName);
				MyToast.show(TalkingGroupActivity.this, toastMsg,
						Toast.LENGTH_SHORT);
				break;
			default:
				MyToast.show(TalkingGroupActivity.this, R.string.hangup_failed,
						Toast.LENGTH_SHORT);
				break;
			}
		}

		private void updateHangUpUI() {
			String userName = selectedMember.get(Attendee.username.name());
			Map<String, String> attendee = new HashMap<String, String>();
			attendee.put(Attendee.username.name(), userName);
			attendee.put(Attendee.telephone_status.name(),
					Attendee.PhoneStatus.Terminated.name());
			memberListAdatper.updateMember(attendee);
		}
	};

	private void kickout(String targetUserName) {
		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.sending_request));
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("dstUserName", targetUserName);
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.kickout_url),
				PostRequestFormat.URLENCODED, params, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedKickout);
	}

	private OnHttpRequestListener onFinishedKickout = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			String userName = selectedMember.get(Attendee.username.name());
			memberListAdatper.removeMember(userName);
		}

		@Override
		public void onForbidden(HttpResponseResult responseResult) {
			dismissProgressDlg();
			String userName = selectedMember.get(Attendee.username.name());

			String toastMsg = String.format(
					getString(R.string.kickout_is_forbidden_for_sb), userName);
			MyToast.show(TalkingGroupActivity.this, toastMsg,
					Toast.LENGTH_SHORT);
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this, R.string.kickout_failed,
					Toast.LENGTH_SHORT);
		}
	};

	private void sendSMS(List<String> numbers) {
		if (numbers != null && numbers.size() > 0) {
			StringBuffer toNumbers = new StringBuffer();
			for (String number : numbers) {
				toNumbers.append(number).append(';');
			}
			Uri uri = Uri.parse("smsto:" + toNumbers.toString());
			Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
			String smsBody = String.format(getString(R.string.invite_sms_body),
					groupId);
			intent.putExtra("sms_body", smsBody);
			startActivity(intent);
		}
	}

	private NotifierCallbackListener notifyCallbackListener = new NotifierCallbackListener() {

		@Override
		public void doAction(String event, JSONObject data) {
			Log.d(SystemConstants.TAG, "NotifierCallbackListener - doAction: "
					+ data.toString());
			if (event.equals(Notify.notice.name())) {
				// process notice message
				try {
					String cmd = data.getString(Notify.cmd.name());
					final JSONArray noticeList = data
							.getJSONArray(Notify.notice_list.name());
					if (cmd.equals(Notify.notify.name())
							|| cmd.equals(Notify.cache.name())) {
						handler.post(new Runnable() {

							@Override
							public void run() {
								for (int i = 0; i < noticeList.length(); i++) {
									// process one notice message
									try {
										processOneNotice(noticeList
												.getJSONObject(i));
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}

							}
						});
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		}
	};

	// do actual processing for each notice
	private void processOneNotice(JSONObject notice) throws JSONException {
		String action = notice.getString(Notify.action.name());
		String groupId = notice.getString(TalkGroup.conferenceId.name());
		if (this.groupId.equals(groupId)) {
			if (Notify.Action.update_status.name().equals(action)) {
				JSONObject attendee = notice.getJSONObject(Attendee.attendee
						.name());
				if (!isOwner()) {
					updateDialButtonStatus(attendee);
				}
				memberListAdatper.updateMember(attendee);
			} else if (Notify.Action.update_attendee_list.name().equals(action)) {
				refreshMemberList();
			} else if (Notify.Action.kickout.name().equals(action)) {
				String accountName = UserManager.getInstance().getUser()
						.getName();
				String attendeeName = notice
						.getString(Attendee.username.name());
				if (accountName.equals(attendeeName)) {
					// kick myself
					AlertDialog.Builder builder = new AlertDialog.Builder(
							TalkingGroupActivity.this).setMessage(
							getString(R.string.you_have_been_kicked_out))
							.setPositiveButton(R.string.ok,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											leaveGroupTalk();
										}
									});
					builder.show();
				} else {
					// update attendee list
					String toastMsg = String.format(
							getString(R.string.sb_is_kicked_out),
							AppUtil.getDisplayName(attendeeName));
					MyToast.show(TalkingGroupActivity.this, toastMsg,
							Toast.LENGTH_SHORT);
					refreshMemberList();
				}
			} else if (Notify.Action.conf_destoryed.name().equals(action)) {
				if (!isOwner()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							TalkingGroupActivity.this)
							.setTitle(R.string.alert_title)
							.setMessage(
									getString(R.string.talkgroup_has_been_closed))
							.setPositiveButton(R.string.ok,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											leaveGroupTalk();
										}
									});
					builder.show();
				}
			}
		}
	}

	private void callAllMember() {
		progressDlg = ProgressDialog.show(TalkingGroupActivity.this, null,
				getString(R.string.sending_request));
		HashMap<String, String> param = new HashMap<String, String>();
		param.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.call_all_url),
				PostRequestFormat.URLENCODED, param, null,
				HttpRequestType.ASYNCHRONOUS, onFinishedCallAllMember);
	}

	private OnHttpRequestListener onFinishedCallAllMember = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this, R.string.calling_all,
					Toast.LENGTH_SHORT);
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this, R.string.call_all_failed,
					Toast.LENGTH_SHORT);
		}
	};

	private void updateDialButtonStatus(JSONObject attendee) {
		try {
			Button dialButton = (Button) findViewById(R.id.gt_dial_bt);
			String dialText = dialButton.getText().toString();
			String accountName = UserManager.getInstance().getUser().getName();
			String attendeeName = attendee.getString(Attendee.username.name());
			if (accountName.equals(attendeeName)) {
				String phoneStatus = attendee
						.getString(Attendee.telephone_status.name());
				if (Attendee.PhoneStatus.Failed.name().equals(phoneStatus)) {
					if (dialText.equals(getString(R.string.calling_in))) {
						// set dial button as dial when call into talking group
						// failed
						setDialButtonAsDial();
						MyToast.show(this, R.string.call_in_failed,
								Toast.LENGTH_SHORT);
					}
				} else if (Attendee.PhoneStatus.Terminated.name().equals(
						phoneStatus)) {
					setDialButtonAsDial();
				} else if (Attendee.PhoneStatus.Established.name().equals(
						phoneStatus)) {
					setDialButtonAsHangupTalking();
				} else if (Attendee.PhoneStatus.CallWait.name().equals(
						phoneStatus)) {
					setDialButtonAsCalling();
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void setDialButtonAsDial() {
		Button dialButton = (Button) findViewById(R.id.gt_dial_bt);
		dialButton.setText(R.string.dial);
	}

	private void setDialButtonAsCalling() {
		Button dialButton = (Button) findViewById(R.id.gt_dial_bt);
		dialButton.setText(R.string.calling_in);
	}

	private void setDialButtonAsHangupTalking() {
		Button dialButton = (Button) findViewById(R.id.gt_dial_bt);
		dialButton.setText(R.string.hangup_talking);
	}

	private void callMeIntoGroupTalking() {
		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.sending_request));
		String accountName = UserManager.getInstance().getUser().getName();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("dstUserName", accountName);
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.call_url), PostRequestFormat.URLENCODED,
				params, null, HttpRequestType.ASYNCHRONOUS, onFinishedCallMeIn);
	}

	private OnHttpRequestListener onFinishedCallMeIn = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			String accountName = UserManager.getInstance().getUser().getName();
			Map<String, String> attendee = new HashMap<String, String>();
			attendee.put(Attendee.username.name(), accountName);
			attendee.put(Attendee.telephone_status.name(),
					Attendee.PhoneStatus.CallWait.name());
			memberListAdatper.updateMember(attendee);
			setDialButtonAsCalling();
		}

		@Override
		public void onForbidden(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this,
					R.string.call_is_forbidden_for_you, Toast.LENGTH_SHORT);
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this, R.string.call_in_failed,
					Toast.LENGTH_SHORT);
		}
	};

	private void hangMeUpFromGroupTalking() {
		progressDlg = ProgressDialog.show(this, null,
				getString(R.string.sending_request));
		String accountName = UserManager.getInstance().getUser().getName();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("dstUserName", accountName);
		params.put(TalkGroup.conferenceId.name(), groupId);
		HttpUtils.postSignatureRequest(getString(R.string.server_url)
				+ getString(R.string.hangup_url), PostRequestFormat.URLENCODED,
				params, null, HttpRequestType.ASYNCHRONOUS, onFinishedHangMeUp);
	}

	private OnHttpRequestListener onFinishedHangMeUp = new OnHttpRequestListener() {

		@Override
		public void onFinished(HttpResponseResult responseResult) {
			dismissProgressDlg();
			updateUI();
		}

		@Override
		public void onForbidden(HttpResponseResult responseResult) {
			dismissProgressDlg();
			MyToast.show(TalkingGroupActivity.this,
					R.string.hangup_is_forbidden_for_you, Toast.LENGTH_SHORT);
		}

		@Override
		public void onFailed(HttpResponseResult responseResult) {
			dismissProgressDlg();

			int status = responseResult.getStatusCode();
			switch (status) {
			case 409:
				updateUI();
				break;

			default:
				MyToast.show(TalkingGroupActivity.this,
						R.string.hangup_talking_failed, Toast.LENGTH_SHORT);
				break;
			}
		}

		private void updateUI() {
			String accountName = UserManager.getInstance().getUser().getName();
			Map<String, String> attendee = new HashMap<String, String>();
			attendee.put(Attendee.username.name(), accountName);
			attendee.put(Attendee.telephone_status.name(),
					Attendee.PhoneStatus.Terminated.name());
			memberListAdatper.updateMember(attendee);
			setDialButtonAsDial();
		}
	};

	private void dismissProgressDlg() {
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			refreshMemberList();
		}
	}

	class HeartBeatTimerTask extends TimerTask {

		@Override
		public void run() {
			// send heart beat to server
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TalkGroup.conferenceId.name(), groupId);
			HttpUtils.postSignatureRequest(getString(R.string.server_url)
					+ getString(R.string.heart_beat_url),
					PostRequestFormat.URLENCODED, params, null,
					HttpRequestType.ASYNCHRONOUS, null);
		}

	}

	private void showVideoView() {
		flipper.setInAnimation(this, R.anim.left_in);
		flipper.setOutAnimation(this, R.anim.left_out);
		flipper.showNext();
	}

	private void showMemberListView() {
		flipper.setInAnimation(this, R.anim.right_in);
		flipper.setOutAnimation(this, R.anim.right_out);
		flipper.showPrevious();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Log.d(SystemConstants.TAG, "e1 x: " + e1.getX() + " e2 x: " + e2.getX());
		if (e1.getX() - e2.getX() > MIN_FLING_DISTANCE) {
			if (currentView == GTViewType.MemberListView) {
				showVideoView();
			}
			currentView = GTViewType.VideoView;
			return true;
		} else if (e2.getX() - e1.getX() > MIN_FLING_DISTANCE) {
			if (currentView == GTViewType.VideoView) {
				showMemberListView();
			}
			currentView = GTViewType.MemberListView;
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

}
