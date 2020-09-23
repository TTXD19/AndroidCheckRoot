package com.android.practice.androidcheckroot;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.scottyab.rootbeer.Const;
import com.scottyab.rootbeer.RootBeer;
import com.scottyab.rootbeer.util.QLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import static com.scottyab.rootbeer.Const.BINARY_SU;

public class MainActivity extends AppCompatActivity {

    private static String LOG_TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /**
         * 使用CSDN的Root檢測方法
         * 網站：https://blog.csdn.net/lintax/article/details/70988565?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-1.channel_param
         */
        if (isDeviceRooted()) {
            Log.d(LOG_TAG, "Your device is rooted");
        } else {
            Log.d(LOG_TAG, "Your device is NOT rooted");
        }





        /**
         * 使用Github第三方Library Root Beer測試
         * 經由Debugger測試，Root也是經由檔案路徑檢測SU關鍵字才檢測出ROOT，測試結果符合預期
         * 網站：https://github.com/scottyab/rootbeer
         */
        RootBeer rootBeer = new RootBeer(this);
        Boolean result = rootBeer.checkForBinary(BINARY_SU);
        Log.d("Main", result.toString());
        if (rootBeer.isRooted()){
            Toast.makeText(MainActivity.this, "Device is rooted", Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(MainActivity.this, "Device is not rooted", Toast.LENGTH_LONG).show();
        }

    }






    public static boolean isDeviceRooted() {
        //if (checkSuperuserApk()){return true;}//Superuser.apk
        //if (checkBusybox()){return true;}

        //if (checkDeviceDebuggable()){return true;}//check buildTags

        if (checkRootPathSU()){return true;}//find su in some path-->唯一測成功的Code
        //if (checkGetRootAuth()){return true;}//exec su
        //if (checkRootWhichSU()){return true;}//find su use 'which'

        if (checkAccessRootData()){return true;}//find su use 'which'


        return false;
    }


    /**
     * checkSuperuserApk與checkBusybox是使用檢查是否有下載SuperSU與Busybox的方法檢測，Emulator與實機測試都不符合結果
     *
     * @return
     */
    public static boolean checkSuperuserApk() {
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                Log.i(LOG_TAG, "/system/app/Superuser.apk exist");
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static synchronized boolean checkBusybox() {
        try {
            Log.i(LOG_TAG, "to exec busybox df");
            String[] strCmd = new String[]{"busybox", "df"};
            ArrayList<String> execResult = executeCommand(strCmd);
            if (execResult != null) {
                Log.i(LOG_TAG, "execResult=" + execResult.toString());
                return true;
            } else {
                Log.i(LOG_TAG, "execResult=null");
                return false;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        }
    }

    /**
     *查看发布的系统版本，是test-keys（测试版），还是release-keys（发布版）。返回结果是“release-keys”，代表此系统是正式发布版。
     * 若是非官方发布版，很可能是完全root的版本，存在使用风险。
     * 在代码中的检测方法如下：
     *
     * Emulator與實機測試都為無效
     * Emulator顯示為dev-keys
     * 實機顯示為release-keys
     *
     * @return
     */
    public static boolean checkDeviceDebuggable() {
        String buildTags = android.os.Build.TAGS;
        Log.d(LOG_TAG, "buildTags=" + buildTags);
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }
        return false;
    }

    /**
     * su是Linux下切换用户的命令，在使用时不带参数，就是切换到超级用户。通常我们获取root权限，就是使用su命令来实现的，所以可以检查这个命令是否存在。
     * 有三个方法来测试su是否存在：
     */

    //检测在常用目录下是否存在su。这个方法是检测常用目录，那么就有可能漏过不常用的目录。
    //所以就有了第二个方法，直接使用shell下的命令来查找。
    //Emulator與實機測試都均符合結果
    public static boolean checkRootPathSU() {
        File f = null;
        final String kSuSearchPaths[] = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/"};
        try {
            for (int i = 0; i < kSuSearchPaths.length; i++) {
                f = new File(kSuSearchPaths[i] + "su");
                if (f != null && f.exists()) {
                    Log.i(LOG_TAG, "find su in : " + kSuSearchPaths[i]);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //which是linux下的一个命令，可以在系统PATH变量指定的路径中搜索某个系统命令的位置并且返回第一个搜索结果。
    //Emulator與實機測試都均不符合結果，但把Which去掉即符合
    public static boolean checkRootWhichSU() {
        String[] strCmd = new String[]{"/system/xbin/which", "su"};
        ArrayList<String> execResult = executeCommand(strCmd);
        if (execResult != null) {
            Log.i(LOG_TAG, "execResult=" + execResult.toString());
            return true;
        } else {
            Log.i(LOG_TAG, "execResult=null");
            return false;
        }
    }

    //由于上面两种查找方法都存在可能查不到的情况，以及有su文件与设备root的差异，
    // 所以，有这第三中方法：我们执行这个命令su。这样，系统就会在PATH路径中搜索su，
    // 如果找到，就会执行，执行成功后，就是获取到真正的超级权限了。
    // 結果：無效，因為Permission直接被Denied
    public static synchronized boolean checkGetRootAuth() {
        Process process = null;
        DataOutputStream os = null;
        try {
            Log.i(LOG_TAG, "to exec su");
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            Log.i(LOG_TAG, "exitValue=" + exitValue);
            if (exitValue == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<String> executeCommand(String[] shellCmd) {
        String line = null;
        ArrayList<String> fullResponse = new ArrayList<String>();
        Process localProcess = null;
        try {
            Log.i(LOG_TAG, "to shell exec which for find su :");
            localProcess = Runtime.getRuntime().exec(shellCmd);
        } catch (Exception e) {
            return null;
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(localProcess.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
        try {
            while ((line = in.readLine()) != null) {
                Log.i(LOG_TAG, "–> Line received: " + line);
                fullResponse.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(LOG_TAG, "–> Full response was: " + fullResponse);
        return fullResponse;
    }

    /**
     * 在Android系统中，有些目录是普通用户不能访问的，例如 /data、/system、/etc 等。
     * 我们就已/data为例，来进行读写访问。本着谨慎的态度，我是先写入一个文件，然后读出，查看内容是否匹配，若匹配，才认为系统已经root了。
     * Emulator與實機測試均不符合結果
     * @return
     */
    public static synchronized boolean checkAccessRootData() {
        try {
            Log.i(LOG_TAG, "to write /data");
            String fileContent = "test_ok";
            Boolean writeFlag = writeFile("/data/su_test", fileContent);
            if (writeFlag) {
                Log.i(LOG_TAG, "write ok");
            } else {
                Log.i(LOG_TAG, "write failed");
            }

            Log.i(LOG_TAG, "to read /data");
            String strRead = readFile("/data/su_test");
            Log.i(LOG_TAG, "strRead=" + strRead);
            if (fileContent.equals(strRead)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        }
    }

    //写文件
    public static Boolean writeFile(String fileName, String message) {
        try {
            FileOutputStream fout = new FileOutputStream(fileName);
            byte[] bytes = message.getBytes();
            fout.write(bytes);
            fout.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //读文件
    public static String readFile(String fileName) {
        File file = new File(fileName);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            while ((len = fis.read(bytes)) > 0) {
                bos.write(bytes, 0, len);
            }
            String result = new String(bos.toByteArray());
            Log.i(LOG_TAG, result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}