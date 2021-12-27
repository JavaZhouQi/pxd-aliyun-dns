package cn.pixiaodan.aliyun.dns;

import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import com.aliyun.alidns20150109.Client;
import com.aliyun.alidns20150109.models.*;

import com.aliyun.teaopenapi.models.*;

public class AliyunDnsApp {

    private static final Log log = LogFactory.get();
    private static String ipCache = "192.168.1.1";
    private static Client client;

    static {
        Config config = new Config()
                // 您的AccessKey ID
                .setAccessKeyId("LTAI5tHJGauTYh2APfQu5BNc")
                // 您的AccessKey Secret
                .setAccessKeySecret("UNFfVUeYvsB2MuqcHv0rEps7hnHDMo");
        // 访问的域名
        config.endpoint = "alidns.cn-shenzhen.aliyuncs.com";
        try {
            client = new Client(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                task();
            }
        }, 0, 1000);
    }

    /**
     * 获取公网ip
     *
     * @return
     */
    public static String getIp() {
        return HttpUtil.get("https://ifconfig.co/ip");
    }

    public static void task() {
        // 开始获取ip
        String ip = getIp();
        log.info("当前ip：{}，缓存ip:{}", ip, ipCache);
        if (Objects.equals(ip, ipCache)) {
            return;
        }
        log.info("ip发生改变，开始修改域名解析配置");
        // 开始修改解析ip
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        describeDomainRecordsRequest.setDomainName("pixiaodan.cn");
        DescribeDomainRecordsResponse describeDomainRecordsResponse;
        try {
            describeDomainRecordsResponse = client.describeDomainRecords(describeDomainRecordsRequest);
        } catch (Exception e) {
            log.error("调用域名解析列表查询错误", e);
            return;
        }
        DescribeDomainRecordsResponseBody body = describeDomainRecordsResponse.getBody();
        if (body.getTotalCount() <= 0) {
            // 当域名没解析的时候不做处理
            return;
        }
        List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> record = body.getDomainRecords().getRecord();
        List<DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord> recordList = record.stream().filter(entity -> Objects.equals(entity.getValue(), ipCache)).collect(Collectors.toList());
        // 开始修改域名ip
        for (DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord describeDomainRecordsResponseBodyDomainRecordsRecord : recordList) {
            UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
            updateDomainRecordRequest.setRecordId(describeDomainRecordsResponseBodyDomainRecordsRecord.getRecordId());
            updateDomainRecordRequest.setRR(describeDomainRecordsResponseBodyDomainRecordsRecord.getRR());
            updateDomainRecordRequest.setType(describeDomainRecordsResponseBodyDomainRecordsRecord.getType());
            updateDomainRecordRequest.setValue(ip);
            try {
                UpdateDomainRecordResponse updateDomainRecordResponse = client.updateDomainRecord(updateDomainRecordRequest);
            } catch (Exception e) {
                log.debug("调用域名解析修改错误", e);
            }
        }
        // ip替换
        ipCache = ip;
    }
}
