package com.shanyangcode.tianmu.job;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.shanyangcode.tianmu.esdao.UserEsDao;
import com.shanyangcode.tianmu.model.entity.User;
import com.shanyangcode.tianmu.model.entity.UserStats;
import com.shanyangcode.tianmu.model.es.UserEs;
import com.shanyangcode.tianmu.service.UserService;
import com.shanyangcode.tianmu.service.UserStatsService;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FullSyncUserToEs implements CommandLineRunner {

    @Resource
    private UserService userService;

    @Resource
    private UserEsDao userEsDao;

    @Resource
    private UserStatsService userStatsService;

    @Override
    public void run(String... args) {
        // 全量获取用户（数据量不大的情况下使用）
        List<User> userList = userService.list();
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        Set<Long> activeUserIds = userList.stream().map(User::getUserId).collect(Collectors.toSet());
        List<Long> staleUserIds = StreamSupport.stream(userEsDao.findAll().spliterator(), false)
                .map(UserEs::getId)
                .filter(id -> !activeUserIds.contains(id))
                .collect(Collectors.toList());
        if (!staleUserIds.isEmpty()) {
            userEsDao.deleteAllById(staleUserIds);
        }
        Map<Long, UserStats> statsMap = userStatsService.listByIds(
                        userList.stream().map(User::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserStats::getUserId, Function.identity()));

        // 转为包含基本资料和统计数据的完整 ES 文档
        List<UserEs> userEsList = userList.stream().map(user -> {
            UserEs userEs = new UserEs();
            userEs.setId(user.getUserId());
            userEs.setNickname(user.getNickname());
            userEs.setAvatar(user.getAvatar());
            userEs.setDescription(user.getDescription());
            UserStats stats = statsMap.get(user.getUserId());
            userEs.setFollowers(stats == null ? 0 : stats.getFollowers());
            userEs.setVideoCount(stats == null ? 0 : stats.getVideoCount());
            return userEs;
        }).collect(Collectors.toList());


        // 分页批量插入到 ES
        final int pageSize = 500;
        int total = userList.size();
        log.info("FullSyncUserToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 注意同步的数据下标不能超过总数据量
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            userEsDao.saveAll(userEsList.subList(i, end));
        }
        log.info("FullSyncUserToEs end, total {}", total);
    }
}
