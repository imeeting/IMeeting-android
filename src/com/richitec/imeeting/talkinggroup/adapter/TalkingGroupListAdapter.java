package com.richitec.imeeting.talkinggroup.adapter;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.richitec.commontoolkit.addressbook.AddressBookManager;
import com.richitec.imeeting.R;
import com.richitec.imeeting.constants.Attendee;
import com.richitec.imeeting.constants.TalkGroup;
import com.richitec.imeeting.util.AppUtil;

public class TalkingGroupListAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private List<JSONObject> groups;

	public TalkingGroupListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		groups = new ArrayList<JSONObject>();
	}
	
	public void setGroupList(JSONArray groupArray) {
		groups.clear();
		if (groupArray != null) {
			for (int i = 0; i < groupArray.length(); i++) {
				try {
					JSONObject obj = groupArray.getJSONObject(i);
					groups.add(obj);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		notifyDataSetChanged();
	}

	public void appendGroupList(JSONArray groupArray) {
		if (groupArray != null) {
			for (int i = 0; i < groupArray.length(); i++) {
				try {
					JSONObject obj = groupArray.getJSONObject(i);
					groups.add(obj);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		notifyDataSetChanged();
	}
	
	public void removeItem(Object item) {
		boolean result =groups.remove(item);
		if (result) {
			notifyDataSetChanged();
		}
	}
	
	public void clear() {
		groups.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return groups.size();
	}

	@Override
	public Object getItem(int position) {
		Object obj = null;
		obj = groups.get(position);
		return obj;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = inflater.inflate(R.layout.talking_group_history_list_item_layout, null);

			viewHolder.titleTV = (TextView) convertView
					.findViewById(R.id.talkingGroupTitle_textView);
			viewHolder.createdTimeTV = (TextView) convertView.findViewById(R.id.talkingGroup_createdTime_textView);
			viewHolder.memberRow = (TableRow) convertView.findViewById(R.id.talkingGroup_attendees_tableRow);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		JSONObject groupItem = (JSONObject) getItem(position);

		try {
			viewHolder.titleTV.setText(groupItem.getString(TalkGroup.title
					.name()));
			
			long createdTime = groupItem.getLong(TalkGroup.created_time.name());
			CharSequence date = DateFormat.format("yyyy-MM-dd hh:mmaa", createdTime * 1000);
			viewHolder.createdTimeTV.setText(date);
			
			JSONArray attendees = groupItem.getJSONArray(TalkGroup.attendees.name());
			for (int i = 0; i < viewHolder.memberRow.getVirtualChildCount(); i++) {
				// get table row item
				View _tableRowItem = viewHolder.memberRow.getVirtualChildAt(i);
				_tableRowItem.setVisibility(View.VISIBLE);
				// check visible view
				if (i < attendees.length()) {
					// check table row item type
					// linearLayout
					JSONObject attendee = attendees.getJSONObject(i);
					if (_tableRowItem instanceof RelativeLayout) {
						// set attendee name
						((TextView) ((RelativeLayout) _tableRowItem)
								.findViewById(R.id.attendee_name_textView))
								.setText(AppUtil.getDisplayNameFromAttendee(attendee));
						String userName = attendee.getString(Attendee.username.name());
						Bitmap avatar = AppUtil.getAvatar(userName);
						ImageView imgView = (ImageView) _tableRowItem.findViewById(R.id.attendee_avatar_imageView);
						if (avatar != null) {
							imgView.setImageBitmap(avatar);
						} else {
							imgView.setImageResource(R.drawable.default_avatar);
						}
					}
				} else {
					_tableRowItem.setVisibility(View.GONE);
				}
			}
			
			String status = groupItem.getString(TalkGroup.status.name());
			if (status.equals("OPEN")) {
				viewHolder.titleTV.setTextColor(0xff8fbc8f);
			} else {
				viewHolder.titleTV.setTextColor(0xffa3a3a3);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return convertView;
	}

	final class ViewHolder {
		public TextView titleTV;
		public TextView createdTimeTV;
		public TableRow memberRow;
	}
}
