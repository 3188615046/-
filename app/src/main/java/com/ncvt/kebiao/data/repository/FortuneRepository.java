package com.ncvt.kebiao.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.ncvt.kebiao.model.DailyFortune;
import java.time.LocalDate;
import java.util.Random;

public final class FortuneRepository {
    private final SharedPreferences prefs;
    private static final int[] WEIGHTS = {0, 1, 0, 2, 3, 2, 4, 4, 5, 5, 6, 7, 8};
    private static final DailyFortune[] FORTUNES = {
            new DailyFortune("great_sakura", "大吉", "神樱映福", "云开月朗，前路自明。",
                    "今天适合主动推进想做的事，回应通常会比你预想更快一些。",
                    "宜：确认安排、完成作业、找老师或同学沟通。",
                    "幸运提示：靠窗的位置、暖色穿搭、先做最难的一件事。",
                    "提醒：好运不是替你完成，而是推你先迈出第一步。"),
            new DailyFortune("great_lantern", "大吉", "御灯长明", "心灯不灭，百事逢春。",
                    "整体节奏很顺，尤其适合处理拖延已久的任务，越早开始越轻松。",
                    "宜：提交申请、整理计划、开启新的学习目标。",
                    "幸运提示：上午九点前动手，今天的状态会更稳。",
                    "提醒：把关键事项写下来，比临时想起来更容易成。"),
            new DailyFortune("good_breeze", "吉", "风起青叶", "微风入袖，所行有成。",
                    "平稳而有收获的一天，专注完成眼前事，就会有意料之外的小惊喜。",
                    "宜：复习、复盘、联系同学、补齐细节。",
                    "幸运提示：绿色或白色的小物件，会让你更沉得住气。",
                    "提醒：别被别人的进度打乱自己的节奏。"),
            new DailyFortune("good_sunrise", "吉", "晴枝照庭", "日色明净，静中生喜。",
                    "不一定一鸣惊人，但今天很适合慢慢把事情做扎实，后劲很足。",
                    "宜：整理笔记、推进小组任务、预约未来几天的安排。",
                    "幸运提示：把手机放远一点，专注力会明显变好。",
                    "提醒：先完成再优化，比一直等待完美时机更有效。"),
            new DailyFortune("mid_moon", "中吉", "月渡长阶", "缓行亦稳，渐近佳境。",
                    "事情会一点点变顺，适合按计划推进，不必急着一步到位。",
                    "宜：按部就班完成清单、处理惯常事务。",
                    "幸运提示：中午前后容易得到有用消息，记得留意通知。",
                    "提醒：保持耐心，今天的运气更偏向稳扎稳打。"),
            new DailyFortune("small_petals", "小吉", "花影轻摇", "一枝初放，喜在细微。",
                    "好运藏在小事里，也许不是轰轰烈烈的一天，但会有温柔的顺利。",
                    "宜：打扫桌面、补交小事项、给自己留一点休息时间。",
                    "幸运提示：先完成最容易拖延的那件小事，整天都会轻很多。",
                    "提醒：别忽视细节，今天的收获往往来自细小的决定。"),
            new DailyFortune("late_glow", "末吉", "暮霞留彩", "迟来之喜，终归有声。",
                    "前半天可能平平，但只要不松劲，后面会慢慢找到状态。",
                    "宜：补进度、做收尾、把散乱的事情重新排好顺序。",
                    "幸运提示：下午和傍晚更容易进入节奏，别太早否定今天。",
                    "提醒：晚一点见效，不代表今天没有好运。"),
            new DailyFortune("calm_pool", "平", "静水观心", "无波亦安，自有分寸。",
                    "今天更适合求稳，不必强行追求惊喜，把基础打牢就是收获。",
                    "宜：整理资料、按时上课、处理日常琐事。",
                    "幸运提示：少开几个并行任务，会比硬撑着同时做更轻松。",
                    "提醒：普通的一天也值得认真过，它会替你攒下底气。"),
            new DailyFortune("small_warning", "小凶", "雨落空阶", "浮念易乱，宜缓不宜急。",
                    "今天容易被琐事打断，计划外的小状况可能多一点，放慢节奏会更好。",
                    "宜：少立 flag、多检查、重要事情预留缓冲时间。",
                    "幸运提示：出门前和提交前多看一遍，能避开不少小麻烦。",
                    "提醒：这不是坏日子，只是更适合稳一点、细一点。")
    };

    public FortuneRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("daily_fortune", Context.MODE_PRIVATE);
    }

    public DailyFortune getTodayFortune() { return getTodayFortune(LocalDate.now()); }

    public DailyFortune getTodayFortune(LocalDate today) {
        if (!today.toString().equals(prefs.getString("draw_date", null))) return null;
        String savedId = prefs.getString("fortune_id", null);
        for (DailyFortune fortune : FORTUNES) {
            if (fortune.id.equals(savedId)) return fortune;
        }
        return null;
    }

    public DailyFortune drawTodayFortune() { return drawTodayFortune(LocalDate.now()); }

    public DailyFortune drawTodayFortune(LocalDate today) {
        synchronized (FortuneRepository.class) {
            DailyFortune existing = getTodayFortune(today);
            if (existing != null) return existing;
            DailyFortune fortune = FORTUNES[WEIGHTS[new Random().nextInt(WEIGHTS.length)]];
            prefs.edit().putString("draw_date", today.toString()).putString("fortune_id", fortune.id).apply();
            return fortune;
        }
    }
}
