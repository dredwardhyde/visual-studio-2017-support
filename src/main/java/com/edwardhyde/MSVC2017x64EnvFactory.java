package com.edwardhyde;

import org.codehaus.mojo.natives.EnvFactory;
import org.codehaus.mojo.natives.NativeBuildException;
import org.codehaus.mojo.natives.msvc.EnvStreamConsumer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

@SuppressWarnings("unused")
public class MSVC2017x64EnvFactory implements EnvFactory {
    @Override
    public Map<String, String> getEnvironmentVariables() {
        String vcInstallDir = null;
        if (!System.getProperty("os.name").contains("Windows")) return null;
        try {
            Process p = Runtime.getRuntime().exec("reg.exe query \"HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\VisualStudio\\SxS\\VS7\" /v 15.0");
            p.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                if (!line.contains("HKEY_LOCAL_MACHINE") && !line.trim().isEmpty()) {
                    String[] res = line.split(" {4}");
                    vcInstallDir = res[res.length - 1];
                    break;
                }
            }
            input.close();
        } catch (Exception e) {
            throw new NativeBuildException("Unable to construct Visual Studio install directory", e);
        }
        if (vcInstallDir == null) return null;
        Map<String, String> result = null;
        try {
            File tmpFile = File.createTempFile("msenv", ".bat");
            StringBuilder buffer = new StringBuilder();
            buffer.append("@echo off\r\n");
            buffer.append("call \"").append(vcInstallDir).append("\"").append("\\VC\\Auxiliary\\Build\\vcvarsall.bat x64\n\r");
            buffer.append("echo " + EnvStreamConsumer.START_PARSING_INDICATOR).append("\r\n");
            buffer.append("set\n\r");
            FileUtils.fileWrite(tmpFile.getAbsolutePath(), buffer.toString());
            Commandline cl = new Commandline();
            cl.setExecutable(tmpFile.getAbsolutePath());
            StreamConsumer stderr = new DefaultConsumer();
            EnvStreamConsumer stdout = new EnvStreamConsumer();
            CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            result = stdout.getParsedEnv();
        } catch (Exception e) {
            throw new NativeBuildException("Unable to execute Visual Studio vcvarsall.bat x64", e);
        }
        return result;
    }
}
