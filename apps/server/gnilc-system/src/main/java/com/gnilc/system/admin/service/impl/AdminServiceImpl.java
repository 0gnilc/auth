package com.gnilc.system.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.servlet.context.DefaultAccessPrincipalHolder;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.utils.BeanCopyUtils;
import com.gnilc.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.system.admin.dao.AdminDao;
import com.gnilc.system.session.AdminSessionManager;
import com.gnilc.system.session.AdminSessionTokenPair;
import com.gnilc.system.admin.entity.bo.AdminBo;
import com.gnilc.system.admin.entity.dto.AdminDto;
import com.gnilc.system.admin.entity.dto.AdminPageDto;
import com.gnilc.system.admin.entity.dto.AdminRoleDto;
import com.gnilc.system.admin.entity.vo.AdminTokenVo;
import com.gnilc.system.admin.entity.vo.AdminVo;
import com.gnilc.system.admin.service.AdminService;
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
        AdminBo bo = getAdminByUsername(username);
        if (bo == null || Boolean.FALSE.equals(bo.getStatus())) {
            return null;
        }
        if (!PASSWORD_ENCODER.matches(password, bo.getPassword())) {
            return null;
        }
        AdminSessionTokenPair pair = sessionManager.createSession(bo.getUserId());
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
        AdminBo bo = getAdminByUserId(userId);
        if (bo == null) {
            return null;
        }
        return toAdminVo(bo, false, true);
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
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "Username is required.");
        Preconditions.checkArgument(StringUtils.isNotBlank(password), "Password is required.");
        validateStrongPassword(password);
        Preconditions.checkArgument(getAdminByUsername(username) == null,
                "An administrator with this username already exists.");
        Long userId = userService.createUser();
        AdminBo bo = new AdminBo();
        bo.setUserId(userId);
        bo.setUsername(username);
        bo.setPassword(PASSWORD_ENCODER.encode(password));
        bo.setNickname(dto.getNickname());
        bo.setAvatar(dto.getAvatar());
        bo.setDescription(dto.getDesc());
        bo.setHomePath(dto.getHomePath());
        bo.setStatus(dto.getStatus());
        save(bo);
        updateRolesIfProvided(userId, dto.getRoleCodes());
    }

    /**
     * 更新管理员资料。
     */
    @Override
    @Transactional
    public void updateAdmin(AdminDto dto) {
        AdminBo bo = getAdmin(dto.getId());
        Preconditions.checkCondition(bo != null, "The administrator no longer exists. Refresh and try again.");
        String username = dto.getUsername();
        if (username != null && !username.equals(bo.getUsername())) {
            Preconditions.checkArgument(getAdminByUsername(username) == null,
                    "An administrator with this username already exists.");
        }
        boolean wasEnabled = Boolean.TRUE.equals(bo.getStatus());
        BeanCopyUtils.copyNonNullProperties(dto, bo);

        // 单独处理密码。
        String password = dto.getPassword();
        if (StringUtils.isNotBlank(password)) {
            validateStrongPassword(password);
            bo.setPassword(PASSWORD_ENCODER.encode(password));
        }

        updateById(bo);
        updateRolesIfProvided(bo.getUserId(), dto.getRoleCodes());

        boolean disabling = wasEnabled && Boolean.FALSE.equals(dto.getStatus());
        if (disabling) {
            sessionManager.cleanupUserSessions(bo.getUserId());
        }
    }

    /**
     * 替换管理员角色。
     */
    @Override
    @Transactional
    public void updateAdminRoles(AdminRoleDto dto) {
        AdminBo bo = getById(dto.getId());
        Preconditions.checkCondition(bo != null, "The administrator no longer exists. Refresh and try again.");
        replaceRoles(bo.getUserId(), dto.getRoleCodes());
    }

    /**
     * 删除管理员。
     */
    @Override
    @Transactional
    public void removeAdmin(Long id) {
        Preconditions.checkArgument(id != null, "An administrator must be selected.");
        AdminBo bo = getById(id);
        Preconditions.checkCondition(bo != null, "The administrator no longer exists. Refresh and try again.");
        sessionManager.cleanupUserSessions(bo.getUserId());
        bo.setUsername(bo.getUsername() + "_del_" + id);
        updateById(bo);
        removeById(id);
        userService.removeUser(bo.getUserId());
        replaceRoles(bo.getUserId(), List.of());
    }

    /**
     * 分页查询管理员。
     */
    @Override
    public PageResult<AdminVo> getAdminPage(AdminPageDto dto) {
        AdminPageDto query = dto == null ? new AdminPageDto() : dto;
        IPage<AdminBo> page = lambdaQuery()
                .eq(StringUtils.isNotBlank(query.getUsername()), AdminBo::getUsername, query.getUsername())
                .like(StringUtils.isNotBlank(query.getNickname()), AdminBo::getNickname, query.getNickname())
                .eq(query.getStatus() != null, AdminBo::getStatus, query.getStatus())
                .orderByDesc(AdminBo::getId)
                .page(query.getPage());
        List<AdminVo> vos = page.getRecords().stream()
                .map(bo -> toAdminVo(bo, true, true))
                .toList();
        return PageResult.of(page, vos);
    }

    /**
     * 解析当前会话 user_id。
     */
    private Long authenticatedUserId() {
        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();
        Preconditions.checkArgument(principal != null, "Your session is no longer valid. Sign in again.");
        String identifier = principal.getIdentifier();
        Preconditions.checkArgument(StringUtils.isNotBlank(identifier),
                "Your session is no longer valid. Sign in again.");
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
    public AdminBo getAdmin(Long id) {
        if (id == null) {
            return null;
        }
        return getById(id);
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
        Preconditions.checkArgument(valid,
                "Password must be 8 to 32 characters and include uppercase and lowercase letters, "
                        + "a number, and a special character, with no whitespace.");
    }

    /**
     * 替换用户角色。
     */
    private void updateRolesIfProvided(Long userId, List<String> roleCodes) {
        if (roleCodes == null) {
            return;
        }
        replaceRoles(userId, roleCodes);
    }

    private void replaceRoles(Long userId, List<String> roleCodes) {
        List<String> codes = roleCodes == null ? List.of() : roleCodes;
        List<Long> roleIds = codes.stream()
                .map(code -> {
                    Preconditions.checkArgument(StringUtils.isNotBlank(code), "Role code is required.");
                    RoleBo bo = roleService.getRoleByCode(code);
                    Preconditions.checkCondition(bo != null,
                            "The role no longer exists. Refresh and try again.");
                    return bo.getId();
                })
                .toList();
        UserRoleDto dto = new UserRoleDto();
        dto.setUserId(userId);
        dto.setRoleIds(roleIds);
        userRoleService.updateUserRole(dto);
    }

    private AdminVo toAdminVo(AdminBo bo, boolean includeStatus, boolean includeRoleCodes) {
        AdminVo vo = new AdminVo();
        BeanUtils.copyProperties(bo, vo);
        vo.setDesc(bo.getDescription());
        if (!includeStatus) {
            vo.setStatus(null);
        }
        if (includeRoleCodes) {
            vo.setRoleCodes(getRoleCodes(bo.getUserId()));
        }
        return vo;
    }
}
