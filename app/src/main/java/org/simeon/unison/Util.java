/*
 *     Unison Android App
 *     Copyright (C) 2019 Simeon Krastnikov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.simeon.unison;

import android.app.ActivityManager;
import android.content.Context;

import java.lang.reflect.Field;

import static android.content.Context.ACTIVITY_SERVICE;

public final class Util {

    public static boolean isServiceRunning(Context context, Class<?> cls) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : am.getRunningServices(Integer.MAX_VALUE)) {
            if (info.service.getClassName().equals(cls.getName()))
                return true;
        }
        return false;
    }

    public static int getProcessPid(Process proc) {
        // A ridiculously hacky way to get the PID of process
        Field field;
        try {
            field = proc.getClass().getDeclaredField("pid");
        }
        catch (NoSuchFieldException e) {
            return 0;
        }
        try {
            field.setAccessible(true);
            int pid = (int) field.get(proc);
            field.setAccessible(false);
            return pid;
        }
        catch (IllegalAccessException e) {
            return 0;
        }
    }

}
