// File: src/model/system/security/RolePermissions.java
package src.model.person;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RolePermissions {
    private static final Map<Role, Set<Permission>> rolePermissionsMap = new HashMap<>();

    static {
        // Định nghĩa quyền cho Admin
        rolePermissionsMap.put(Role.ADMIN, EnumSet.allOf(Permission.class)); // Admin có tất cả quyền liên quan đến lớp học

        // Định nghĩa quyền cho Teacher
        rolePermissionsMap.put(Role.TEACHER, EnumSet.of(
                Permission.VIEW_ALL_CLASSES,
                Permission.VIEW_STUDENTS_IN_CLASS,
                Permission.ADD_STUDENT
                // Thêm các quyền khác cho Teacher nếu cần (ví dụ: EDIT_OWN_CLASS)
        ));
    }

    /**
     * Kiểm tra xem vai trò có quyền cụ thể hay không.
     * @param role Vai trò của người dùng.
     * @param permission Quyền cần kiểm tra.
     * @return true nếu vai trò có quyền, false nếu ngược lại.
     */
    public static boolean hasPermission(Role role, Permission permission) {
        Set<Permission> permissions = rolePermissionsMap.get(role);
        return permissions != null && permissions.contains(permission);
    }
}
