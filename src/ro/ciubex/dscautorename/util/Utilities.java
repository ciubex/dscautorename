/**
 * This file is part of DSCAutoRename application.
 *
 * Copyright (C) 2015 Claudiu Ciobotariu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.dscautorename.util;

import android.content.Context;
import android.database.Cursor;
import android.os.IBinder;
import android.util.AndroidRuntimeException;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;

/**
 * This is an utilities class with some useful methods.
 * Created by claudiu on 06.04.2015.
 */
public class Utilities {
	private static final String TAG = Utilities.class.getName();

	static final String SERVICE_MOUNT = "mount";
	public static final String ROOT_ID_PRIMARY_EMULATED = "primary";
	public static final String INVALID_STATE = "invalid_state";
	private static Method METHOD_ServiceManager_getService;
	private static Method METHOD_IMountService_asInterface;
	private static Method METHOD_IMountService_getVolumeList;
	private static Method METHOD_IMountService_getVolumeState;
	private static Method METHOD_IMountService_mountVolume;
	private static Method METHOD_IMountService_unmountVolume;
	private static Method METHOD_IMountService_getStorageUsers;
	private static Method METHOD_IMountService_isUsbMassStorageEnabled;

	private static Method METHOD_StorageVolume_getUuid;
	private static Method METHOD_StorageVolume_getState;
	private static Method METHOD_StorageVolume_getStorageId;
	private static Method METHOD_StorageVolume_getDescriptionId;
	private static Method METHOD_StorageVolume_getDescription;
	private static Method METHOD_StorageVolume_getPathFile;
	private static Method METHOD_StorageVolume_getPath;
	private static Method METHOD_StorageVolume_isRemovable;
	private static Method METHOD_StorageVolume_isPrimary;
	private static Method METHOD_StorageVolume_isEmulated;

	static {
		try {
			Class<?> clazz;

			clazz = Class.forName("android.os.ServiceManager");
			if (clazz != null) {
				METHOD_ServiceManager_getService = clazz.getMethod("getService", String.class);
			}

			clazz = Class.forName("android.os.storage.IMountService$Stub");
			if (clazz != null) {
				METHOD_IMountService_asInterface = clazz.getMethod("asInterface", IBinder.class);
			}

			clazz = Class.forName("android.os.storage.IMountService");
			String methodName;
			if (clazz != null) {
				for (Method method : clazz.getDeclaredMethods()) {
					methodName = method.getName();
					if ("getVolumeList".equals(methodName)) {
						METHOD_IMountService_getVolumeList = method;
					} else if ("getVolumeState".equals(methodName)) {
						METHOD_IMountService_getVolumeState = method;
					} else if ("mountVolume".equals(methodName)) {
						METHOD_IMountService_mountVolume = method;
					} else if ("unmountVolume".equals(methodName)) {
						METHOD_IMountService_unmountVolume = method;
					} else if ("getStorageUsers".equals(methodName)) {
						METHOD_IMountService_getStorageUsers = method;
					} else if ("isUsbMassStorageEnabled".equals(methodName)) {
						METHOD_IMountService_isUsbMassStorageEnabled = method;
					}
				}
			}

			clazz = Class.forName("android.os.storage.StorageVolume");
			if (clazz != null) {
				for (Method method : clazz.getDeclaredMethods()) {
					methodName = method.getName();
					if ("getUuid".equals(methodName)) {
						METHOD_StorageVolume_getUuid = method;
					} else if ("getFsUuid".equals(methodName)) { // API 23
						METHOD_StorageVolume_getUuid = method;
					} else if ("getStorageId".equals(methodName)) {
						METHOD_StorageVolume_getStorageId = method;
					} else if ("getDescription".equals(methodName)) {
						METHOD_StorageVolume_getDescription = method;
					} else if ("getDescriptionId".equals(methodName)) {
						METHOD_StorageVolume_getDescriptionId = method;
					} else if ("getPath".equals(methodName)) {
						METHOD_StorageVolume_getPath = method;
					} else if ("getPathFile".equals(methodName)) {
						METHOD_StorageVolume_getPathFile = method;
					} else if ("isRemovable".equals(methodName)) {
						METHOD_StorageVolume_isRemovable = method;
					} else if ("isPrimary".equals(methodName)) {
						METHOD_StorageVolume_isPrimary = method;
					} else if ("isEmulated".equals(methodName)) {
						METHOD_StorageVolume_isEmulated = method;
					} else if ("getState".equals(methodName)) {
						METHOD_StorageVolume_getState = method;
					}
				}
			}
		} catch (Exception e) {
			Log.wtf(TAG, e);
		}
	}

	private static Object invoke(String methodName, Method method, Object receiver, Object... args) {
		try {
			if (method != null) {
				return method.invoke(receiver, args);
			} else {
				Log.e(TAG, "Method " + methodName + " is null");
			}
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new AndroidRuntimeException(e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new AndroidRuntimeException(e);
		} catch (InvocationTargetException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new AndroidRuntimeException(e);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			throw new AndroidRuntimeException(e);
		}
		return null;
	}

	public static class MountService {

		public static Object getService() {
			Object service = invoke("ServiceManager.getService()",
					METHOD_ServiceManager_getService, null, SERVICE_MOUNT);
			if (service != null) {
				return invoke("IMountService.asInterface()",
						METHOD_IMountService_asInterface, null, service);
			}
			return null;
		}

		public static String getVolumeState(Object mountService, MountVolume volume) {
			String state = INVALID_STATE;
			String mountPoint = volume.getPath();
			try {
				state = (String) invoke("IMountService.getVolumeState()",
						METHOD_IMountService_getVolumeState, mountService, mountPoint);
			} catch (Exception e) {
				Log.e(TAG, "getVolumeState(" + mountPoint + ")", e);
			}
			return state;
		}

		public static int mountVolume(Object mountService, String mountPoint) {
			return (Integer) invoke("IMountService.mountVolume()",
					METHOD_IMountService_mountVolume, mountService, mountPoint);
		}

		public static void unmountVolume(Object mountService, String mountPoint, boolean force) {
			if (METHOD_IMountService_unmountVolume != null) {
				switch (METHOD_IMountService_unmountVolume.getParameterTypes().length) {
					case 1:
						invoke("IMountService.unmountVolume()",
								METHOD_IMountService_unmountVolume, mountService,
								mountPoint);
						break;
					case 2:
						invoke("IMountService.unmountVolume()",
								METHOD_IMountService_unmountVolume, mountService,
								mountPoint, force);
						break;
					case 3:
						invoke("IMountService.unmountVolume()",
								METHOD_IMountService_unmountVolume, mountService,
								mountPoint, force, force);
						break;
				}
			}
		}

		public static int[] getStorageUsers(Object mountService, String path) {
			return (int[]) invoke("IMountService.getStorageUsers()",
					METHOD_IMountService_getStorageUsers, mountService, path);
		}

		public static boolean isUsbMassStorageEnabled(Object mountService) {
			return (Boolean) invoke("IMountService.isUsbMassStorageEnabled()",
					METHOD_IMountService_isUsbMassStorageEnabled, mountService);
		}

		public static String getStorageVolumeDescription(Object obj, Context context) {
			String result = null;
			if (METHOD_StorageVolume_getDescription != null) {
				switch (METHOD_StorageVolume_getDescription.getParameterTypes().length) {
					case 0: result = (String) invoke("StorageVolume.getDescription()",
							METHOD_StorageVolume_getDescription, obj);
						break;
					case 1: result = (String) invoke("StorageVolume.getDescription()",
							METHOD_StorageVolume_getDescription, obj, context);
						break;
				}
			}
			return result;
		}

		public static List<MountVolume> getVolumeList(Object mountService, Context context) {
			Object[] arr = null;
			if (METHOD_IMountService_getVolumeList != null) {
				switch (METHOD_IMountService_getVolumeList.getParameterTypes().length) {
					case 0: arr = (Object[]) invoke("IMountService.getVolumeList()",
							METHOD_IMountService_getVolumeList, mountService);
						break;
					case 3: arr = (Object[]) invoke("IMountService.getVolumeList()",
							METHOD_IMountService_getVolumeList, mountService, 0, "/", 0);
						break;
				}
			}
			return prepareMountVolumes(mountService, arr, context);
		}

		private static List<MountVolume> prepareMountVolumes(Object mountService, Object[] arr, Context context) {
			int len = arr != null ? arr.length : 0;
			List<MountVolume> volumes = new ArrayList<MountVolume>();
			if (len > 0) {
				MountVolume volume;
				for (Object obj : arr) {
					volume = prepareMountVolume(mountService, obj, context);
					if (volume != null) {
						volumes.add(volume);
					}
				}
			} else {
				prepareVolumeStorages(volumes);
			}
			return volumes;
		}

		private static void prepareVolumeStorages(List<MountVolume> volumes) {
			String externalStorage = System.getenv("EXTERNAL_STORAGE");
			String secondaryStorage = System.getenv("SECONDARY_STORAGE");
			MountVolume volume;
			if (!isEmpty(externalStorage)) {
				volume = new MountVolume();
				volume.setPrimary(true);
				volume.setPathFile(new File(externalStorage));
				volumes.add(volume);
			}
			if (!isEmpty(secondaryStorage)) {
				for (String path : secondaryStorage.split(":")) {
					volume = new MountVolume();
					volume.setPathFile(new File(path));
					volumes.add(volume);
				}
			}
		}

		private static MountVolume prepareMountVolume(Object mountService, Object obj, Context context) {
			MountVolume volume = null;
			if ("android.os.storage.StorageVolume".equals(obj.getClass().getName())) {
				try {
					volume = new MountVolume();
					if (METHOD_StorageVolume_getUuid != null) {
						volume.setUuid((String) invoke("StorageVolume.getUuid()",
								METHOD_StorageVolume_getUuid, obj));
					}
					if (METHOD_StorageVolume_getStorageId != null) {
						volume.setStorageId((Integer) invoke("StorageVolume.getStorageId()",
								METHOD_StorageVolume_getStorageId, obj));
					}
					if (METHOD_StorageVolume_getPathFile != null) {
						volume.setPathFile((File) invoke("StorageVolume.getPathFile()",
								METHOD_StorageVolume_getPathFile, obj));
					} else if (METHOD_StorageVolume_getPath != null) {
						String path = (String) invoke("StorageVolume.getPath()",
								METHOD_StorageVolume_getPath, obj);
						volume.setPathFile(new File(path));
					}
					if (METHOD_StorageVolume_isPrimary != null) {
						volume.setPrimary((Boolean) invoke("StorageVolume.isPrimary()",
								METHOD_StorageVolume_isPrimary, obj));
					}
					if (METHOD_StorageVolume_isEmulated != null) {
						volume.setEmulated((Boolean) invoke("StorageVolume.isEmulated()",
								METHOD_StorageVolume_isEmulated, obj));
					}
					if (METHOD_StorageVolume_isRemovable != null) {
						volume.setRemovable((Boolean) invoke("StorageVolume.isRemovable()",
								METHOD_StorageVolume_isRemovable, obj));
					}
					if (METHOD_StorageVolume_getDescriptionId != null) {
						volume.setDescriptionId((Integer) invoke("StorageVolume.getDescriptionId()",
								METHOD_StorageVolume_getDescriptionId, obj));
					} else if (METHOD_StorageVolume_getDescription != null) {
						volume.setDescription(getStorageVolumeDescription(obj, context));
					}
					if (METHOD_StorageVolume_getState != null) {
						volume.setState((String) invoke("StorageVolume.getState()",
								METHOD_StorageVolume_getState, obj));
					} else if (METHOD_IMountService_getVolumeState != null) {
						volume.setState(getVolumeState(mountService, volume));
					}
					if (volume.getUuid() == null && volume.isPrimary() && volume.isEmulated()) {
						volume.setUuid(ROOT_ID_PRIMARY_EMULATED);
					}
				} catch (Exception e) {
					Log.e(TAG, "Exception: " + e.getMessage() + " volume: " + volume, e);
					throw new AndroidRuntimeException(e);
				}
			}
			return volume;
		}
	}

	/**
	 * Check if two strings are contained each other.
	 *
	 * @param string1 First string to check.
	 * @param string2 Second string to check.
	 * @return True if one string is contained into the other.
	 */
	public static boolean contained(String string1, String string2) {
		if (string1.length() > string2.length()) {
			return (string1.indexOf(string2) > -1);
		}
		return (string2.indexOf(string1) > -1);
	}

	/**
	 * Close a closeable object.
	 *
	 * @param closeable Object to be close.
	 */
	public static void doClose(Object closeable) {
		if (closeable instanceof Closeable) {
			try {
				((Closeable) closeable).close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception e) {
				Log.e(TAG, "doClose Exception: " + e.getMessage(), e);
			}
		} else if (closeable instanceof Cursor) {
			try {
				((Cursor) closeable).close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception e) {
				Log.e(TAG, "doClose Exception: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Returns true if the object is null or is empty.
	 *
	 * @param object The object to be examined.
	 * @return True if object is null or zero length.
	 */
	public static boolean isEmpty(Object object) {
		if (object instanceof CharSequence)
			return ((CharSequence) object).length() == 0;
		return object == null;
	}

	/**
	 * Parse a string date time value in format yyyy:MM:dd HH:mm:ss to a date.
	 * @param dateTime Date time to be parsed.
	 * @return The parsed date time.
	 */
	public static Date parseExifDateTimeString(String dateTime) {
		String[] arr = dateTime.split(" ");
		Date date = null;
		if (arr.length == 2) {
			String[] dateString = arr[0].split(":");
			String[] timeString = arr[1].split(":");
			if (dateString.length == 3 && timeString.length == 3) {
				Calendar calendar = GregorianCalendar.getInstance();
				calendar.set(Calendar.YEAR, parseToInt(dateString[0]));
				calendar.set(Calendar.MONTH, parseToInt(dateString[1]) - 1);
				calendar.set(Calendar.DAY_OF_MONTH, parseToInt(dateString[2]));
				calendar.set(Calendar.HOUR_OF_DAY, parseToInt(timeString[0]));
				calendar.set(Calendar.MINUTE, parseToInt(timeString[1]));
				calendar.set(Calendar.SECOND, parseToInt(timeString[2]));
				calendar.set(Calendar.MILLISECOND, 0);
				date = calendar.getTime();
			}
		}
		return date;
	}

	/**
	 * Parse a string to int. If string can not be parsed -1 is returned.
	 *
	 * @param value The string value to be parsed.
	 * @return The parsed value or -1 if can not be parsed.
	 */
	public static int parseToInt(String value) {
		int result = -1;
		try {
			result = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			Log.e(TAG, "Value:" + value);
		}
		return result;
	}

	/**
	 * Check if the move files is enabled.
	 * @return The move files enabled state.
	 */
	public static boolean isMoveFiles(SelectedFolderModel selectedFolder) {
		return selectedFolder != null &&
				!Utilities.isEmpty(selectedFolder.getFullPath());
	}
}
