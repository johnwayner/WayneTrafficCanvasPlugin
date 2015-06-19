package com.johnwayner.plugins;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by johnwayner on 6/18/15.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressWarnings("DefaultFileTemplate")
public class WayneTrafficCanvasPlugin extends PebbleCanvasPlugin {

    public static final int TRAFFIC_WARNING_PLUGIN_ID = 1;
    private static Pattern trafficRegex = Pattern.compile(".*\"trafficCongestion\":\"(.*?)\".*");
    
    private static Map<String, RouteInformation> currentTraffic = new HashMap<>();
    private static Date lastUpdateDate = null;
    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.US);

    private static final ScheduledExecutorService scheduleTaskExecutor;

    static {
        currentTraffic.put("%360", new RouteInformation("Displays current traffic congestion on 360 N.",
                                                        "%360",
                                                        "http://dev.virtualearth.net/REST/V1/Routes/Driving?wp.0=30.3639034,-97.7899801&wp.1=30.3887598,-97.7469462&key=Au4mPxUIpEbz8UBYVCE3FnSalv2dq9SrjKYlhdQfCvHX82xpu7dNEy9MPXbJIHCO"));
        currentTraffic.put("%2222", new RouteInformation("Displays current traffic congestion on 2222 E.",
                                                        "%2222",
                                                         "http://dev.virtualearth.net/REST/V1/Routes/Driving?wp.0=30.3589162,-97.7883471&wp.1=30.3367509,-97.7561606&key=Au4mPxUIpEbz8UBYVCE3FnSalv2dq9SrjKYlhdQfCvHX82xpu7dNEy9MPXbJIHCO"));

        scheduleTaskExecutor= Executors.newScheduledThreadPool(5);

        // This schedule a task to run every 10 minutes:
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                lastUpdateDate = new Date();
                for(RouteInformation routeInformation : currentTraffic.values()) {
                    try {
                        String content = convertStreamToString(routeInformation.url.openStream());
                        Matcher matcher = trafficRegex.matcher(content);
                        if(matcher.matches()) {
                            routeInformation.currentTraffic = matcher.group(1);
                        }

                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                
                notify_canvas_updates_available(TRAFFIC_WARNING_PLUGIN_ID, WayneTrafficCanvasApplication.getContext());
            }
        }, 0, 10, TimeUnit.MINUTES);
    }
    
    @Override
    protected ArrayList<PluginDefinition> get_plugin_definitions(Context context) {
        ArrayList<PluginDefinition> pluginDefinitions = new ArrayList<>();

        TextPluginDefinition trafficWarningPlugin = new TextPluginDefinition();
        trafficWarningPlugin.id = TRAFFIC_WARNING_PLUGIN_ID;
        trafficWarningPlugin.name = "Traffic Warnings";

        trafficWarningPlugin.format_masks = new ArrayList<>();
        trafficWarningPlugin.format_mask_descriptions = new ArrayList<>();
        trafficWarningPlugin.format_mask_examples = new ArrayList<>();

        for(Map.Entry<String, RouteInformation> entry : currentTraffic.entrySet()) {
            trafficWarningPlugin.format_masks.add(entry.getKey());
            trafficWarningPlugin.format_mask_descriptions.add(entry.getValue().maskDescription);
            trafficWarningPlugin.format_mask_examples.add(entry.getValue().maskExample);
        }

        trafficWarningPlugin.format_masks.add("%t");
        trafficWarningPlugin.format_mask_descriptions.add("Last update time.");
        trafficWarningPlugin.format_mask_examples.add("%t");

        trafficWarningPlugin.default_format_string = "%360";
        pluginDefinitions.add(trafficWarningPlugin);

        return pluginDefinitions;
    }
    
    @Override
    protected String get_format_mask_value(int def_id, String format_mask, Context context, String param) {
        if(def_id == TRAFFIC_WARNING_PLUGIN_ID) {

            RouteInformation routeInformation = currentTraffic.get(format_mask);
            if(null != routeInformation) {
                return routeInformation.currentTraffic;
            } else if("%t".equals(format_mask)) {
                if(null != lastUpdateDate) {
                    return dateFormat.format(lastUpdateDate);
                } else {
                    return "N/A";
                }
            } else {
                return "ERR";
            }
        }
        return "";
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    protected Bitmap get_bitmap_value(int def_id, Context context, String param) {
        return null;
    }
    
    public static final class RouteInformation {
        public String maskDescription;
        public String maskExample;
        public URL url;
        public String currentTraffic = "N/A";

        public RouteInformation(String maskDescription, String maskExample, String url) {
            this.maskDescription = maskDescription;
            this.maskExample = maskExample;
            try {
                this.url = new URL(url);
            } catch(MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
