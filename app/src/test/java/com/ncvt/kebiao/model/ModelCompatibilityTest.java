package com.ncvt.kebiao.model;

import com.ncvt.kebiao.util.CourseDisplayUtils;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static org.junit.Assert.*;

public class ModelCompatibilityTest {
    @Test
    public void changingSemesterPreservesDisplayAndLoginSettings() {
        AppSettings.Builder builder = new AppSettings.Builder();
        builder.account = "demo";
        builder.password = "demo-password";
        builder.nightModeEnabled = true;
        builder.deepCourseCardEnabled = true;
        builder.showNonCurrentWeekCourses = false;
        AppSettings original = builder.build();
        AppSettings.Builder updated = original.toBuilder();
        updated.academicYear = "2026";
        updated.semester = "3";
        AppSettings settings = updated.build();
        assertTrue(settings.isValid());
        assertTrue(settings.nightModeEnabled);
        assertTrue(settings.deepCourseCardEnabled);
        assertFalse(settings.showNonCurrentWeekCourses);
        assertEquals("demo", settings.account);
        assertEquals("", original.academicYear);
    }

    @Test
    public void courseSurvivesDialogSerialization() throws Exception {
        Course original = course("A101");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(original); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Course restored = (Course) input.readObject();
            assertEquals(original, restored);
            assertEquals(original.hashCode(), restored.hashCode());
        }
    }

    @Test
    public void changedCourseDetailsInvalidateAdapterContent() {
        assertNotEquals(course("A101"), course("B201"));
        assertEquals("第1-2节", CourseDisplayUtils.periodLabel(course("A101")));
        assertEquals("周一", CourseDisplayUtils.dayOfWeekLabel(1));
        assertEquals("2", CourseDisplayUtils.semesterDisplay("12"));
    }

    private Course course(String classroom) {
        return new Course(7, "Java", "Teacher", classroom, 1, 1, 2, "1-16", 0xFFDFF3E4, "2026", "3");
    }
}
