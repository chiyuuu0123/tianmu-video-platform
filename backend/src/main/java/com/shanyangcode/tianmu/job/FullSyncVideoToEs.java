package com.shanyangcode.tianmu.job;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.shanyangcode.tianmu.esdao.VideoEsDao;
import com.shanyangcode.tianmu.model.entity.Video;
import com.shanyangcode.tianmu.model.entity.User;
import com.shanyangcode.tianmu.model.entity.VideoStats;
import com.shanyangcode.tianmu.model.es.VideoEs;
import com.shanyangcode.tianmu.service.UserService;
import com.shanyangcode.tianmu.service.VideoService;
import com.shanyangcode.tianmu.service.VideoStatsService;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FullSyncVideoToEs implements CommandLineRunner {

    @Resource
    private VideoService videoService;

    @Resource
    private VideoEsDao videoEsDao;

    @Resource
    private UserService userService;

    @Resource
    private VideoStatsService videoStatsService;

    @Override
    public void run(String... args) {
        // 全量获取视频（数据量不大的情况下使用）
        List<Video> videoList = videoService.list();
        if (CollUtil.isEmpty(videoList)) {
            return;
        }
        Set<Long> activeVideoIds = videoList.stream().map(Video::getVideoId).collect(Collectors.toSet());
        List<Long> staleVideoIds = StreamSupport.stream(videoEsDao.findAll().spliterator(), false)
                .map(VideoEs::getId)
                .filter(id -> !activeVideoIds.contains(id))
                .collect(Collectors.toList());
        if (!staleVideoIds.isEmpty()) {
            videoEsDao.deleteAllById(staleVideoIds);
        }
        Map<Long, User> userMap = userService.listByIds(
                        videoList.stream().map(Video::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        Map<Long, VideoStats> statsMap = videoStatsService.listByIds(
                        videoList.stream().map(Video::getVideoId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(VideoStats::getVideoId, Function.identity()));

        // 转为完整 ES 文档，保证搜索卡片所需字段不会为空
        List<VideoEs> videoEsList = videoList.stream().map(video -> {
            VideoEs videoEs = new VideoEs();
            videoEs.setId(video.getVideoId());
            videoEs.setTitle(video.getTitle());
            videoEs.setCoverUrl(video.getCoverUrl());
            videoEs.setCreateTime(video.getCreateTime());
            videoEs.setDuration(video.getDuration());
            videoEs.setFileUrl(video.getFileUrl());
            videoEs.setUserId(video.getUserId());
            User user = userMap.get(video.getUserId());
            videoEs.setNickName(user == null ? null : user.getNickname());
            VideoStats stats = statsMap.get(video.getVideoId());
            videoEs.setViewCount(stats == null ? 0 : stats.getViewCount());
            videoEs.setBulletCount(stats == null ? 0 : stats.getBulletCount());
            return videoEs;
        }).collect(Collectors.toList());
        // 分页批量插入到 ES
        final int pageSize = 500;
        int total = videoList.size();
        log.info("FullSyncVideoToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 注意同步的数据下标不能超过总数据量
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            videoEsDao.saveAll(videoEsList.subList(i, end));
        }
        log.info("FullSyncVideoToEs end, total {}", total);
    }
}
