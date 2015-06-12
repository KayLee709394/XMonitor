package com.XMonitor.curator.zk;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ksy on 2015/6/11.
 */
public class ZookeeperHost {

    public String hostName;
    public int port;
    private boolean problem;

    private static final Logger log =  LoggerFactory.getLogger(ZookeeperHost.class);

    public ZookeeperHost(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;

    }

    @Override
    public String toString() {
        return hostName + ":" + port;
    }

    public void pulse() {
        String reply = command(Commands.RuokCommand);
        if (!"imok".equals(reply)) {
            String warnMsg = String.format("zookeeper host[%s] ping reply: [%s]", toString(), reply);
            //发送警告
//            Utils.sendNotify("zookeeper", warnMsg);
            log.warn(warnMsg);
        }
    }

    public boolean isOk() {
        return "imok".equals(command(Commands.RuokCommand));
    }

    public JsonValues getInfo() {
        String infoMsg = command(Commands.MntrCommand);
        Map<String, String> metrics = Maps.newHashMap();
        for (String kvStr : infoMsg.split("\n")) {
            if (kvStr.isEmpty()) {
                continue;
            }
            String[] kv = kvStr.split("\t");
            if (kv.length == 2) {
                metrics.put(kv[0], kv[1]);
            }
        }

        metrics.put("isOk", isOk() ? "ok" : "error");
        metrics.put("host", toString());

        return JsonValues.of(
                metrics,
                "host",
                "isOk",
                "zk_server_state",
                "zk_version",
                "zk_avg_latency",
                "zk_max_latency",
                "zk_min_latency",
                "zk_packets_received",
                "zk_packets_sent",
                "zk_outstanding_requests",
                "zk_znode_count",
                "zk_watch_count",
                "zk_ephemerals_count",
                "zk_approximate_data_size",
                "zk_followers",
                "zk_synced_followers",
                "zk_pending_syncs",
                "zk_open_file_descriptor_count",
                "zk_max_file_descriptor_count"
        );
    }

    public String command(String command) {
        // Try 10 times if we face "Connection reset" error.
        String resp = null;
        for (int i = 0; i < 10; i++) {
            resp = doCommand(command);
            if (!"Connection reset".equals(resp)) {
                return resp;
            }
        }
        return resp;
    }

    public String doCommand(String command) {
        Socket s = null;
        try {
            s = new Socket();
            s.setSoLinger(false, 10);
            s.setSoTimeout(20000);
            s.connect(new InetSocketAddress(hostName, port));
            IOUtils.write(command, s.getOutputStream());
            return IOUtils.toString(s.getInputStream());
        } catch (IOException e) {
            log.warn(e+"send 4TLR command [%s] to [%s:%d] error"+command+hostName, port);
            return e.getMessage();
        } finally {
            IOUtils.closeQuietly(s);
        }
    }
}
