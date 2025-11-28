package top.lyh.entity.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * SRS 回调参数DTO（Data Transfer Object）
 * 对应 SRS 回调时发送的 JSON 参数格式，字段与 SRS 官方回调参数一致
 */
@Data // Lombok 注解，自动生成 getter/setter/toString/equals 等方法
public class SrsCallbackDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SRS 应用名（对应 srs.conf 中的 vhost 下的 app 配置，如你代码中的 "live"）
     */
    private String app;

    /**
     * 流名（即推流密钥 streamKey，对应直播间的 streamKey 字段）
     */
    private String stream;

    /**
     * 推流/拉流的客户端 IP 地址（如主播的 IP、观众的 IP）
     */
    private String client_ip;

    /**
     * 推流/拉流的客户端端口号
     */
    private Integer client_port;

    /**
     * SRS 服务器的 IP 地址（虚拟机中 SRS 绑定的 IP）
     */
    private String server_ip;

    /**
     * SRS 服务器的端口号（如 RTMP 1935、HTTP 8080）
     */
    private Integer server_port;

    /**
     * 推流地址中携带的额外参数（query string）
     * 例：推流地址 ?auth_key=xxx&expire=xxx 时，param 就是 "auth_key=xxx&expire=xxx"
     * 仅 on_publish 回调会携带（用于鉴权参数提取）
     */
    private String param;

    /**
     * 协议类型（如 "rtmp"、"http-flv"、"hls"）
     */
    private String schema;

    /**
     * 流的唯一标识（SRS 内部生成，格式：app/stream）
     */
    private String stream_id;

    /**
     * DVR 录制文件路径（仅 on_dvr 回调会携带）
     * 例：/data/srs/dvr/live/streamKey/2025-11-26/12-30-45.mp4
     */
    private String file;

    /**
     * DVR 录制文件大小（单位：字节，仅 on_dvr 回调会携带）
     */
    private Long file_size;

    /**
     * DVR 录制时长（单位：秒，仅 on_dvr 回调会携带）
     */
    private Float duration;

    /**
     * 鉴权 Token（从 param 中提取的 auth_key，非 SRS 原生字段，自定义存入）
     */
    private String token;

    /**
     * 过期时间戳（从 param 中提取的 expire，非 SRS 原生字段，自定义存入）
     */
    private String expire;
}