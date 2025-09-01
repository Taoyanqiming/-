import { createRouter, createWebHistory } from 'vue-router';
import LoginVue from '../views/Login.vue';

// 用户相关组件导入
import UserLayout from '../views/User/UserLayout.vue';
import UserHome from '../views/User/Home.vue';
import Mall from '../views/User/Mall.vue';
import Reservation from '../views/User/Reservation.vue';
import UserForum from '../views/User/Forum.vue';
import User from '../views/User/User.vue'; 
import Messages from '../views/User/Messages.vue';


// 新增子路由组件
import UserPostsComponent from '../components/User/UserPostsComponent.vue'; // 个人发帖组件
import UserFavoritesComponent from '../components/User/UserFavoritesComponent.vue'; // 用户收藏组件
import UserEditComponent from '../components/User/UserEditComponent.vue'; // 修改信息组件
import MyReservations from '../components/appoint/MyReservations.vue'; // 我的预约组件
import CreateReservation from '../components/appoint/CreateReservation.vue'; // 创建预约组件
import UserForumDetail from '../components/forum/PostDetail.vue'; // 论坛详情页
import AppointDetail from '../components/appoint/AppointmentDetail.vue'; // 预约详情页
import List from '../components/mall/product.vue'; // 商品列表组件
import SeckList from '../components/mall/seckill.vue'; // 秒杀商品列表组件
import CreateForum from '../components/forum/CreateForum.vue'; // 创建帖子组件
import SeckListDetail from '../components/mall/DeProduct.vue'; // 秒杀商品详情组件
import ListDetail from '../components/mall/Detail.vue'; // 商品详情组件
import CreateMall from '../components/mall/createProduct.vue'; // 创建商品组件
import OrderList from '../components/mall/orderList.vue'; // 订单列表组件
// 定义路由关系
const routes = [
  { path: '/', redirect: '/user/home' }, // 根路径重定向到用户主页
  { path: '/login', component: LoginVue }, // 登录页
  {
    path: '/user',
    component: UserLayout, // 用户布局组件（包含导航栏和公共内容）
    redirect: '/user/home', // 默认子路由（访问 /user 时自动跳转）
    children: [
      // 原有子路由
      { path: 'home', component: UserHome, name: 'UserHome' }, // 用户主页
      { path: 'mall', component: Mall, name: 'UserMall' ,
         redirect: '/user/mall/product',
         children: [
          {path:'order',component:OrderList},
          { path: 'product', component: List }, 
          { path: 'seckProduct', component: SeckList }, 
          { path: 'seckProduct/info/:seckillId', component: SeckListDetail, name: 'SeckListDetail' }, // 秒杀商品详情
          { path: 'product/info/:productId', component: ListDetail , name: 'ListDetail' }, // 商品详情
          

        ]
      }, // 商城
      {path:"create/product",component:CreateMall},
      { 
         path: 'reservation', 
         component: Reservation,
         name: 'Reservation' ,
         redirect: '/user/reservation/info',
          children: [
          { path: 'info', component: MyReservations }, //列表
          { path:':appointmentId', component: AppointDetail},//详情
          { path: 'create', component: CreateReservation }, //创建预约
        ]
      }, // 预约
      { path: 'forum', component: UserForum, name: 'UserForum' }, // 论坛
      { path: 'forum/:postId', component: UserForumDetail, name: 'UserForumDetail' }, // 论坛详情页
      { path:'create/forum', component: CreateForum, name: 'UserForumCreate'}, // 创建帖子
      { path: 'messages', component: Messages, name: 'UserMessages' }, // 消息中心
      {
        path: 'info', 
        component: User,
        redirect: '/user/info/posts', // 默认子路由（访问 /user/info 时自动跳转）
        children: [
          { path: 'posts', component: UserPostsComponent }, // 个人发帖
          { path: 'favorites', component: UserFavoritesComponent }, // 用户收藏
          { path: 'edit', component: UserEditComponent } // 修改信息
        ]
      },
    ]
  }
];

// 创建路由器
const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;