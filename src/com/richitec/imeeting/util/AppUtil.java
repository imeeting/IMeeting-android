package com.richitec.imeeting.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.richitec.commontoolkit.addressbook.AddressBookManager;
import com.richitec.imeeting.constants.Attendee;

public class AppUtil {
	public static String getDisplayNameFromAttendee(JSONObject attendee) {
		String displayName = "";
		if (attendee != null) {
			try {
				String userName = attendee.getString(Attendee.username.name());
				String nickname = attendee.getString(Attendee.nickname.name());

				List<String> contactDisplayNames = AddressBookManager
						.getInstance().getContactsDisplayNamesByPhone(userName);
				if (contactDisplayNames.size() > 0) {
					displayName = contactDisplayNames.get(0);
				}
				if (userName.equals(displayName) && nickname != null
						&& !nickname.equals("")) {
					displayName = nickname;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				try {
					displayName = attendee.getString(Attendee.username.name());
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}

		}

		return displayName;
	}

	public static String getDisplayNameFromAttendee(Map<String, String> attendee) {
		String displayName = "";
		if (attendee != null) {
			String userName = attendee.get(Attendee.username.name());
			String nickname = attendee.get(Attendee.nickname.name());
			List<String> contactDisplayNames = AddressBookManager.getInstance()
					.getContactsDisplayNamesByPhone(userName);
			if (contactDisplayNames.size() > 0) {
				displayName = contactDisplayNames.get(0);
			}
			if (userName.equals(displayName) && nickname != null
					&& !nickname.equals("")) {
				displayName = nickname;
			}
		}
		return displayName;
	}

	public static String getDisplayName(String phoneNumber) {
		String displayName = phoneNumber;
		List<String> contactDisplayNames = AddressBookManager.getInstance()
				.getContactsDisplayNamesByPhone(phoneNumber);
		if (contactDisplayNames.size() > 0) {
			displayName = contactDisplayNames.get(0);
		}
		return displayName;
	}

	private static HashMap<String, Bitmap> avatarMap = new HashMap<String, Bitmap>();

	public static Bitmap getAvatar(String phoneNumber) {
		Bitmap avatar = avatarMap.get(phoneNumber);
		if (avatar == null) {
			List<byte[]> photos = AddressBookManager.getInstance()
					.getContactsPhotosByPhone(phoneNumber);
			if (photos != null && photos.size() > 0) {
				byte[] photoBytes = photos.get(0);
				if (photoBytes != null) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							photoBytes);
					avatar = BitmapFactory.decodeStream(bis);
					try {
						bis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					avatarMap.put(phoneNumber, avatar);
				}
			}
		}
		return avatar;
	}
}
