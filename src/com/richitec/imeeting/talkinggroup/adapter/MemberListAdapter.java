package com.richitec.imeeting.talkinggroup.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.richitec.commontoolkit.user.UserManager;
import com.richitec.imeeting.R;
import com.richitec.imeeting.constants.Attendee;
import com.richitec.imeeting.talkinggroup.statusfilter.IStatusFilter;
import com.richitec.imeeting.util.AppUtil;

public class MemberListAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private List<Map<String, String>> memberList;
	private IStatusFilter filter;

	public MemberListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		memberList = new ArrayList<Map<String, String>>();
	}

	public List<Map<String, String>> getMemberList() {
		return memberList;
	}
	
	public void setStatusFilter(IStatusFilter filter) {
		this.filter = filter;
	}

	private Map<String, String> copyJsonAttendee(JSONObject attendee) {
		Map<String, String> attendeeMap = new HashMap<String, String>();
		try {
			String name = attendee.getString(Attendee.username.name());
			attendeeMap.put(Attendee.username.name(), name);
		} catch (JSONException e) {
		}

		try {
			String nickname = attendee.getString(Attendee.nickname.name());
			attendeeMap.put(Attendee.nickname.name(), nickname);
		} catch (JSONException e) {
		}
		
		try {
			String onlineStatus = attendee.getString(Attendee.online_status
					.name());
			attendeeMap.put(Attendee.online_status.name(), onlineStatus);
		} catch (JSONException e) {
		}

		try {
			String videoStatus = attendee.getString(Attendee.video_status
					.name());
			attendeeMap.put(Attendee.video_status.name(), videoStatus);
		} catch (JSONException e) {
		}

		try {
			String phoneStatus = attendee.getString(Attendee.telephone_status
					.name());
			attendeeMap.put(Attendee.telephone_status.name(), phoneStatus);
		} catch (JSONException e) {
		}
		return attendeeMap;
	}

	public void setData(JSONArray membersJsonArray) {
		memberList.clear();
		if (membersJsonArray != null) {
			String accountName = UserManager.getInstance().getUser().getName();
			for (int i = 0; i < membersJsonArray.length(); i++) {
				try {
					JSONObject memJsonObject = membersJsonArray
							.getJSONObject(i);

					Map<String, String> member = copyJsonAttendee(memJsonObject);
					String name = member.get(Attendee.username.name());
					if (accountName.equals(name)) {
						memberList.add(0, member);
					} else {
						memberList.add(member);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		notifyDataSetChanged();
	}

	public void updateMember(JSONObject attendeeJson) {
		Map<String, String> attendee = copyJsonAttendee(attendeeJson);
		updateMember(attendee);
	}
	
	public void updateMember(Map<String, String> attendee) {
		String attendeeName = attendee.get(Attendee.username.name());
		if (attendeeName == null) {
			return;
		}
		for (Map<String, String> member : memberList) {
			String memberName = member.get(Attendee.username.name());
			if (attendeeName.equals(memberName)) {
				// find member to update
				if (filter != null) {
					Map<String, String> filteredAttendee = filter.filterStatus(attendee, member);
					member.putAll(filteredAttendee);
				} else {
					member.putAll(attendee);
				}
				notifyDataSetChanged();
				break;
			}
		}
	}
	
	public void removeMember(String userName) {
		for (Map<String, String> member : memberList) {
			String memberName = member.get(Attendee.username.name());
			if (memberName.equals(userName)) {
				memberList.remove(member);
				notifyDataSetChanged();
				break;
			}
		}
	}

	@Override
	public int getCount() {
		return memberList.size();
	}

	@Override
	public Object getItem(int position) {
		return memberList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = inflater.inflate(R.layout.member_list_item, null);

			viewHolder.avatarView = (ImageView) convertView
					.findViewById(R.id.member_avatar);
			viewHolder.onlineStatusView = (ImageView) convertView
					.findViewById(R.id.member_online_status);
			viewHolder.nameView = (TextView) convertView
					.findViewById(R.id.member_name);
			viewHolder.phoneStatusIconView = (ImageView) convertView
					.findViewById(R.id.member_phone_status_icon);
			viewHolder.phoneStatusTextView = (TextView) convertView
					.findViewById(R.id.member_phone_status_text);

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		@SuppressWarnings("unchecked")
		Map<String, String> member = (Map<String, String>) getItem(position);

		String displayName = AppUtil.getDisplayNameFromAttendee(member);
		String onlineStatus = member.get(Attendee.online_status.name());
		String telephoneStatus = member.get(Attendee.telephone_status.name());
		
		String userName = member.get(Attendee.username.name());
		Bitmap avatar = AppUtil.getAvatar(userName);
		if (avatar != null) {
			viewHolder.avatarView.setImageBitmap(avatar);
		} else {
			viewHolder.avatarView.setImageResource(R.drawable.default_avatar);
		}
		
		viewHolder.nameView.setText(displayName);

		if (Attendee.OnlineStatus.online.name().equals(onlineStatus)) {
			viewHolder.onlineStatusView
					.setImageResource(R.drawable.online_flag);

		} else {
			viewHolder.onlineStatusView
					.setImageResource(R.drawable.offline_flag);
		}

		if (Attendee.PhoneStatus.Terminated.name().equals(telephoneStatus)) {
			viewHolder.phoneStatusIconView.setImageDrawable(null);
			viewHolder.phoneStatusTextView.setText("");
		} else if (Attendee.PhoneStatus.CallWait.name().equals(telephoneStatus)) {
			viewHolder.phoneStatusIconView.setImageResource(R.drawable.calling);
			viewHolder.phoneStatusTextView.setText(R.string.calling);
		} else if (Attendee.PhoneStatus.Established.name().equals(
				telephoneStatus)) {
			viewHolder.phoneStatusIconView
					.setImageResource(R.drawable.intalking);
			viewHolder.phoneStatusTextView.setText(R.string.talking);
		} else if (Attendee.PhoneStatus.Failed.name().equals(telephoneStatus)) {
			viewHolder.phoneStatusIconView
					.setImageResource(R.drawable.call_failed);
			viewHolder.phoneStatusTextView.setText(R.string.call_failed);
		}

		return convertView;
	}

	final class ViewHolder {
		public ImageView avatarView;
		public ImageView onlineStatusView;
		public TextView nameView;
		public ImageView phoneStatusIconView;
		public TextView phoneStatusTextView;
	}

}
