<template>
  <div class="home-container">
    <!-- 第一行：背景图片 -->
    <div class="background-row">
      <img src="@/assets/background.png" alt="背景图片" class="background-img" />
    </div>
    <!-- 第二行：三个模块（添加路由跳转） -->
    <div class="modules-row">
      <router-link to="/user/forum" class="module">
        校园热点
      </router-link>
      <router-link to="/user/mall" class="module">
        校园文创
      </router-link>
      <router-link to="/user/reservation" class="module">
        访客预约
      </router-link>
    </div>
    <!-- 第三行：走马灯轮播图 -->
    <!-- 第三行：走马灯轮播图与日历容器 -->
    <div class="carousel-and-calendar-container">
      <!-- 走马灯轮播图 -->
      <div class="carousel-row">
        <el-carousel indicator-position="outside">
          <el-carousel-item v-for="(item, index) in carouselImages" :key="index">
            <img :src="item" class="carousel-img" />
          </el-carousel-item>
        </el-carousel>
      </div>
      <!-- 日历组件 -->
      <div class="calendar-container">
        <div class="date-display">
          <div class="date-year-month">{{ currentDate.year }}年{{ currentDate.month }}月</div>
          <div class="date-day">{{ currentDate.day }}日 {{ currentDate.weekday }}</div>
        </div>
        <div class="calendar-grid">
          <div class="calendar-header">
            <div class="calendar-weekday">日</div>
            <div class="calendar-weekday">一</div>
            <div class="calendar-weekday">二</div>
            <div class="calendar-weekday">三</div>
            <div class="calendar-weekday">四</div>
            <div class="calendar-weekday">五</div>
            <div class="calendar-weekday">六</div>
          </div>
          <div class="calendar-days" v-for="(week, weekIndex) in calendarDays" :key="weekIndex">
            <div class="calendar-day" v-for="(day, dayIndex) in week" :key="dayIndex"
              :class="{ 'current-day': day === currentDate.day && weekIndex === currentWeekIndex && dayIndex === currentDayIndex }">
              {{ day || '' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 轮播图片示例，可以替换为实际图片路径
const carouselImages = ref([
  new URL('@/assets/map.png', import.meta.url).href,
  new URL('@/assets/pic5.png', import.meta.url).href,
  new URL('@/assets/pic6.png', import.meta.url).href
])

// 日期相关数据
const currentDate = ref({
  year: '',
  month: '',
  day: '',
  weekday: ''
})

const calendarDays = ref([])
const currentWeekIndex = ref(0)
const currentDayIndex = ref(0)

// 获取当前日期并生成日历
const getCurrentDateAndCalendar = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()

  // 星期几数组
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekday = weekdays[now.getDay()]

  // 更新当前日期
  currentDate.value = {
    year,
    month,
    day,
    weekday
  }

  // 生成日历数据
  generateCalendar(year, month, day)
}

// 生成指定年月的日历
const generateCalendar = (year, month, currentDay) => {
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const firstDayOfWeek = firstDay.getDay() // 0-6，0是星期日
  const lastDayOfMonth = lastDay.getDate()

  let calendar = []
  let week = []
  let dayIndex = 0

  // 添加当月第一天之前的空格
  for (let i = 0; i < firstDayOfWeek; i++) {
    week.push('')
    dayIndex++
  }

  // 添加当月的每一天
  for (let i = 1; i <= lastDayOfMonth; i++) {
    week.push(i)

    // 检查是否是当前日期，记录其位置
    if (i === currentDay) {
      currentWeekIndex.value = Math.floor(dayIndex / 7)
      currentDayIndex.value = dayIndex % 7
    }

    dayIndex++

    // 每7天创建一个新周
    if (dayIndex % 7 === 0) {
      calendar.push([...week])
      week = []
    }
  }

  // 如果最后一周不满7天，补全
  if (week.length > 0) {
    while (week.length < 7) {
      week.push('')
    }
    calendar.push([...week])
  }

  calendarDays.value = calendar
}

onMounted(() => {
  getCurrentDateAndCalendar()

  // 每月1日更新日历
  const updateCalendarOnFirstDay = () => {
    const now = new Date()
    if (now.getDate() === 1) {
      getCurrentDateAndCalendar()
    }
  }

  // 每天更新日期显示
  setInterval(() => {
    getCurrentDateAndCalendar()
  }, 24 * 60 * 60 * 1000)
})
</script>

<style scoped>
.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.home-container {
  width: 100%;
  min-height: 100vh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.background-row {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.background-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.modules-row {
  width: 90%;
  display: flex;
  justify-content: space-between;
  margin: 24px 0;
}

/* 添加鼠标指针样式 */
.module {
  flex: 1;
  margin: 0 8px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  padding: 32px 0;
  cursor: pointer;
  /* 添加点击指针 */
  transition: transform 0.3s;
  /* 添加过渡效果 */
}

.module:hover {
  transform: translateY(-5px);
  /* 鼠标悬停时轻微上移 */
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  /* 加深阴影 */
}

.carousel-row {
  width: 90%;
  margin-top: 16px;
}

.carousel-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
}

.carousel-row {
  width: 90%;
  /* 明确设置宽度 */
  margin-top: 16px;
}

.el-carousel {
  width: 100%;
  /* 确保轮播容器占满父容器 */
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  /* 保持图片比例，完整显示 */
  /* object-fit: cover;  /* 如需裁剪填充，可切换此选项 */
  border-radius: 8px;
}

.carousel-and-calendar-container {
  width: 90%;
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  gap: 20px;
  /* 走马灯与日历之间的间距 */
}

.carousel-row {
  width: 70%;
  /* 走马灯占70%宽度 */
}

.calendar-container {
  width: 28%;
  /* 日历占28%宽度，留出2%间隙 */
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.date-display {
  padding: 16px;
  text-align: center;
  background: #409EFF;
  color: white;
  font-size: 18px;
}

.date-year-month {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 5px;
}

.date-day {
  font-size: 16px;
}

.calendar-grid {
  padding: 10px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.calendar-weekday {
  width: 14.28%;
  text-align: center;
  font-weight: bold;
  color: #606266;
  font-size: 14px;
}

.calendar-days {
  display: flex;
  flex-wrap: wrap;
}

.calendar-day {
  width: 14.28%;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.calendar-day:not(:empty):hover {
  background-color: #f5f7fa;
}

.current-day {
  background-color: #409EFF;
  color: white;
  border-radius: 50%;
}
</style>