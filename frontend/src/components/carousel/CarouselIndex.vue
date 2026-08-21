<template>
  <el-carousel trigger="click" arrow="always">
    <el-carousel-item v-for="item in carousels" :key="item.id">
      <a class="carousel-inner" :href="item.target">
        <img :src="item.url" :alt="item.title" @error="handleImageError" />
        <div class="carousel-footer">
          <div class="carousel-title">
            {{ item.title }}
          </div>
          <div class="carousel-mask"></div>
        </div>
      </a>
    </el-carousel-item>
  </el-carousel>
</template>

<script>
import { fetchVideoList } from "@/api/video";
import fallbackBanner from "@/assets/img/sky_banner.png";

export default {
  name: "CarouselIndex",
  data() {
    return {
      // 轮播图列表el-carousel__button
      carousels: [],
    };
  },
  methods: {
    // 请求
    async getCarousels() {
      try {
        const response = await fetchVideoList(1, 6);
        const videos = Array.isArray(response.data) ? response.data : [];
        const availableVideos = videos
          .filter((video) => video.videoId && video.fileUrl && video.coverUrl && video.title)
          .slice(0, 6);
        if (response.code === 200 && availableVideos.length > 0) {
          this.carousels = availableVideos.map((video) => ({
            id: video.videoId,
            url: video.coverUrl,
            title: video.title.trim(),
            color: "#5894d4",
            target: `/video/${video.videoId}`,
          }));
          return;
        }
      } catch (error) {
        // 后端不可用时仍显示本地图片，不再请求旧服务器。
      }
      this.carousels = [{
        id: "fallback",
        url: fallbackBanner,
        title: "欢迎来到天幕",
        color: "#5894d4",
        target: "/",
      }];
    },
    handleImageError(event) {
      const image = event.currentTarget;
      if (image && image.dataset.fallbackApplied !== "true") {
        image.dataset.fallbackApplied = "true";
        image.src = fallbackBanner;
      }
    },
  },
  created() {
    this.getCarousels();
  },
  mounted() {},
  beforeMount() {},
};
</script>

<style scoped>
.carousel-inner {
  height: 100%;
  width: 100%;
  position: relative;
  display: inline-block;
  line-height: 1;
  vertical-align: middle;
  background-color: var(--graph_bg_regular);
  cursor: pointer;
}
img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 确保图片覆盖整个容器 */
  position: absolute;
  top: 0;
  left: 0;
}
::v-deep.el-carousel.el-carousel--horizontal {
  height: 100%;
}
::v-deep .el-carousel__container {
  height: 100%;
}
::v-deep .el-carousel__arrow {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background-color: rgba(255, 255, 255, 0.1);
}
::v-deep .el-carousel__arrow:hover {
  background-color: rgba(255, 255, 255, 0.2);
}
::v-deep .el-carousel__arrow.el-carousel__arrow--right {
  top: unset;
  bottom: 7%;
}
::v-deep .el-carousel__arrow.el-carousel__arrow--left {
  top: unset;
  bottom: 7%;
  left: unset;
  right: 56px;
}
.carousel-footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 80px;
  z-index: 2; /* 确保文字在图片上方 */
}
.carousel-title {
  width: calc(100% - 120px);
  position: relative;
  left: 15px;
  top: 15px;
  z-index: 9;
  color: #fff;
  font-size: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.carousel-footer .carousel-mask {
  width: 100%;
  content: "";
  display: block;
  transition: translate 0s;
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
  user-select: none;
  pointer-events: none;
  background: transparent; /* 改为透明 */
}
@media (min-width: 1367px) and (max-width: 1700.9px) {
  .carousel-footer .carousel-mask {
    height: 810px;
  }
}
::v-deep .el-carousel__indicators--horizontal {
  position: absolute;
  left: 15px;
  bottom: 20px;
  margin: -1.5px;
  z-index: 2;
  display: flex;
  align-items: center;
  transform: unset;
}
::v-deep .el-carousel__indicator.el-carousel__indicator--horizontal {
  margin: 4px;
}
::v-deep .el-carousel__button {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transition: all 0.4s;
}
::v-deep .el-carousel__indicator.is-active button {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background-color: #fff;
}
@media (max-width: 1700.9px) {
  .title span {
    font-size: 18px;
  }
}

@media (min-width: 1701px) {
  .title span {
    font-size: 20px;
  }
}
</style>
