package org.example.selfwebblog.interaction.security;

import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class XdbIpRegionResolver implements IpRegionResolver {
    private static final Logger log = LoggerFactory.getLogger(XdbIpRegionResolver.class);
    private final Ip2Region searcher;

    public XdbIpRegionResolver(@Value("${interaction.region.xdb-path}") String xdbPath) {
        Ip2Region loaded = null;
        Path path = Path.of(xdbPath).toAbsolutePath().normalize();
        if (Files.isRegularFile(path)) {
            try {
                Config config = Config.custom()
                        .setCachePolicy(Config.VIndexCache)
                        .setSearchers(2)
                        .setXdbPath(path.toString())
                        .asV4();
                loaded = Ip2Region.create(config, null);
            } catch (Exception exception) {
                log.warn("IP region database could not be loaded; public region will be unknown");
            }
        } else {
            log.warn("IP region database is absent; public region will be unknown: {}", path);
        }
        this.searcher = loaded;
    }

    @Override
    public String resolve(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) return "未知";
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()) {
                return "本地网络";
            }
            if (searcher == null) return "未知";
            String region = searcher.search(ipAddress);
            return publicRegion(region);
        } catch (Exception exception) {
            return "未知";
        }
    }

    static String publicRegion(String region) {
        if (region == null || region.isBlank()) return "未知";
        String[] fields = region.split("\\|");
        String country = useful(fields, 0);
        String province = useful(fields, 1);
        if (country == null) return province == null ? "未知" : province;
        if (province == null || province.equals(country)) return country;
        return country + " · " + province;
    }

    private static String useful(String[] fields, int index) {
        if (index >= fields.length) return null;
        String value = fields[index].trim();
        return value.isEmpty() || "0".equals(value) ? null : value;
    }

    @PreDestroy
    public void close() {
        if (searcher == null) return;
        try {
            searcher.close();
        } catch (Exception ignored) {
            // Application shutdown should continue even if the lookup resource fails to close.
        }
    }
}
