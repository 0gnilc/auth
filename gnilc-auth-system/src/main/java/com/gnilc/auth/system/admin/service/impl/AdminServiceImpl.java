package com.gnilc.auth.system.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.servlet.context.DefaultAccessPrincipalHolder;
import com.gnilc.auth.authz.rbac.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.common.utils.BeanCopyUtils;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.auth.system.admin.dao.AdminDao;
import com.gnilc.auth.system.session.AdminSessionManager;
import com.gnilc.auth.system.session.AdminSessionTokenPair;
import com.gnilc.auth.system.admin.entity.bo.AdminBo;
import com.gnilc.auth.system.admin.entity.dto.AdminDto;
import com.gnilc.auth.system.admin.entity.dto.AdminPageDto;
import com.gnilc.auth.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.auth.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.auth.system.admin.entity.vo.AdminVo;
import com.gnilc.auth.system.admin.service.AdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


/**
 * 编排后台管理员资料、会话和 RBAC 角色。
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminDao, AdminBo> implements AdminService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final AdminSessionManager sessionManager;
    private final RoleService roleService;
    private final MenuService menuService;
    private final UserService userService;
    private final UserRoleService userRoleService;

    public AdminServiceImpl(AdminSessionManager sessionManager,
                            RoleService roleService,
                            MenuService menuService,
                            UserService userService,
                            UserRoleService userRoleService) {
        this.sessionManager = sessionManager;
        this.roleService = roleService;
        this.menuService = menuService;
        this.userService = userService;
        this.userRoleService = userRoleService;
    }

    /**
     * 登录管理员并创建令牌。
     */
    @Override
    public AdminTokenVo login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }
        AdminBo admin = getAdminByUsername(username);
        if (admin == null || Boolean.FALSE.equals(admin.getStatus())) {
            return null;
        }
        if (!PASSWORD_ENCODER.matches(password, admin.getPassword())) {
            return null;
        }
        AdminSessionTokenPair pair = sessionManager.createSession(admin.getUserId());
        return AdminTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    /**
     * 刷新访问令牌。
     */
    @Override
    public AdminTokenVo refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return null;
        }
        AdminSessionTokenPair pair = sessionManager.refreshSession(refreshToken);
        if (pair == null) {
            return null;
        }
        return AdminTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    /**
     * 登出当前会话。
     */
    @Override
    public boolean logout(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return false;
        }
        return sessionManager.logout(refreshToken);
    }

    /**
     * 查询当前管理员资料。
     */
    @Override
    public AdminVo getUserInfo() {
        Long userId = authenticatedUserId();
        AdminBo admin = getAdminByUserId(userId);
        if (admin == null) {
            return null;
        }
        return toAdminVo(admin, false, true);
    }

    /**
     * 查询当前管理员角色标识。
     */
    @Override
    public List<String> getRoleCodes() {
        return getRoleCodes(authenticatedUserId());
    }

    /**
     * 查询当前管理员按钮访问标识。
     */
    @Override
    public List<String> getMenuAccessCodes() {
        return getMenuAccessCodes(authenticatedUserId());
    }

    /**
     * 根据用户名查询管理员。
     */
    @Override
    public AdminBo getAdminByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        return lambdaQuery().eq(AdminBo::getUsername, username).one();
    }

    /**
     * 查询用户角色标识。
     */
    @Override
    public List<String> getRoleCodes(Long userId) {
        return Optional.ofNullable(roleService.getRoles(userId)).orElse(List.of()).stream()
                .map(RoleBo::getCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 查询用户按钮访问标识。
     */
    @Override
    public List<String> getMenuAccessCodes(Long userId) {
        return Optional.ofNullable(menuService.getMenus(userId)).orElse(List.of()).stream()
                .filter(menu -> menu.getType() == MenuType.BUTTON)
                .filter(MenuBo::getStatus)
                .map(MenuBo::getAccessCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 创建管理员。
     */
    @Override
    @Transactional
    public void createAdmin(AdminDto dto) {
        String username = dto.getUsername();
        String password = dto.getPassword();
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "请输入用户名");
        Preconditions.checkArgument(StringUtils.isNotBlank(password), "请输入密码");
        validateStrongPassword(password);
        Preconditions.checkArgument(getAdminByUsername(username) == null, "用户名已存在");
        Long userId = userService.createUser();
        AdminBo admin = new AdminBo();
        admin.setUserId(userId);
        admin.setUsername(username);
        admin.setPassword(PASSWORD_ENCODER.encode(password));
        admin.setNickname(dto.getNickname());
        admin.setAvatar(dto.getAvatar());
        admin.setDescription(dto.getDesc());
        admin.setHomePath(dto.getHomePath());
        admin.setStatus(dto.getStatus());
        save(admin);
        doUpdateRoles(userId, dto.getRoleCodes());
    }

    /**
     * 更新管理员资料。
     */
    @Override
    @Transactional
    public void updateAdmin(AdminDto dto) {
        AdminBo admin = getAdmin(dto.getId());
        Preconditions.checkCondition(admin != null, "管理员不存在，请刷新后重试");
        if (dto.getUsername() != null) {
            if (!dto.getUsername().equals(admin.getUsername())) {
                Preconditions.checkArgument(getAdminByUsername(dto.getUsername()) == null,
                        "用户名已存在");
            }
        }
        boolean wasEnabled = Boolean.TRUE.equals(admin.getStatus());
        BeanCopyUtils.copyNonNullProperties(dto, admin);

        // 单独处理密码。
        if (StringUtils.isNotBlank(dto.getPassword())) {
            validateStrongPassword(dto.getPassword());
            admin.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
        }

        updateById(admin);
        doUpdateRoles(admin.getUserId(), dto.getRoleCodes());

        boolean disabling = wasEnabled && Boolean.FALSE.equals(dto.getStatus());
        if (disabling) {
            sessionManager.cleanupUserSessions(admin.getUserId());
        }
    }

    /**
     * 替换管理员角色。
     */
    @Override
    @Transactional
    public void updateAdminRoles(AdminRoleDto dto) {
        AdminBo admin = getById(dto.getId());
        Preconditions.checkCondition(admin != null, "管理员不存在，请刷新后重试");
        doReplaceRoles(admin.getUserId(), dto.getRoleCodes());
    }

    /**
     * 删除管理员。
     */
    @Override
    @Transactional
    public void removeAdmin(Long id) {
        Preconditions.checkArgument(id != null, "请选择管理员");
        AdminBo admin = getById(id);
        Preconditions.checkCondition(admin != null, "管理员不存在，请刷新后重试");
        sessionManager.cleanupUserSessions(admin.getUserId());
        admin.setUsername(admin.getUsername() + "_del_" + id);
        updateById(admin);
        removeById(id);
        userService.removeUser(admin.getUserId());
        doReplaceRoles(admin.getUserId(), List.of());
    }

    /**
     * 分页查询管理员。
     */
    @Override
    public PageResult<AdminVo> getAdminPage(AdminPageDto dto) {
        AdminPageDto queryDto = dto == null ? new AdminPageDto() : dto;
        IPage<AdminBo> page = lambdaQuery()
                .eq(StringUtils.isNotBlank(queryDto.getUsername()), AdminBo::getUsername, queryDto.getUsername())
                .like(StringUtils.isNotBlank(queryDto.getNickname()), AdminBo::getNickname, queryDto.getNickname())
                .eq(queryDto.getStatus() != null, AdminBo::getStatus, queryDto.getStatus())
                .orderByDesc(AdminBo::getId)
                .page(queryDto.getPage());
        List<AdminVo> avs = page.getRecords().stream()
                .map(ab -> toAdminVo(ab, true, true)).toList();
        return PageResult.of(page, avs);
    }

    /**
     * 解析当前会话 user_id。
     */
    private Long authenticatedUserId() {
        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();
        Preconditions.checkArgument(principal != null, "请重新登录");
        String identifier = principal.getIdentifier();
        Preconditions.checkArgument(StringUtils.isNotBlank(identifier), "请重新登录");
        return Long.valueOf(identifier);
    }

    @Override
    public AdminBo getAdminByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return lambdaQuery().eq(AdminBo::getUserId, userId).one();
    }

    @Override
    public AdminBo getAdmin(Long adminId) {
        if (adminId == null) {
            return null;
        }
        return getById(adminId);
    }


    /**
     * 校验管理员密码强度。
     */
    private void validateStrongPassword(String password) {
        boolean valid = password != null
                && password.length() >= 8
                && password.length() <= 32
                && password.chars().noneMatch(Character::isWhitespace)
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        Preconditions.checkArgument(valid, "密码必须为8-32位且包含大写字母、小写字母、数字、特殊字符，不能包含空白字符");
    }

    /**
     * 替换用户角色。
     */
    private void doUpdateRoles(Long userId, List<String> roleCodes) {
        if (roleCodes == null) {
            return;
        }
        doReplaceRoles(userId, roleCodes);
    }

    private void doReplaceRoles(Long userId, List<String> roleCodes) {
        List<String> targetRoleCodes = roleCodes == null ? List.of() : roleCodes;
        List<Long> roleIds = targetRoleCodes.stream()
                .map(code -> {
                    Preconditions.checkArgument(StringUtils.isNotBlank(code), "角色标识不能为空");
                    RoleBo role = roleService.getRoleByCode(code);
                    Preconditions.checkCondition(role != null, "角色不存在，请刷新后重试");
                    return role.getId();
                })
                .toList();
        UserRoleDto userRoleDto = new UserRoleDto();
        userRoleDto.setUserId(userId);
        userRoleDto.setRoleIds(roleIds);
        userRoleService.updateUserRole(userRoleDto);
    }

    private AdminVo toAdminVo(AdminBo admin, boolean includeStatus, boolean includeRoleCodes) {
        AdminVo vo = new AdminVo();
        BeanUtils.copyProperties(admin, vo);
        vo.setDesc(admin.getDescription());
        if (!includeStatus) {
            vo.setStatus(null);
        }
        if (includeRoleCodes) {
            vo.setRoleCodes(getRoleCodes(admin.getUserId()));
        }
        return vo;
    }
}
