package com.gnilc.core.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.exception.AuthenticationFailedException;
import com.gnilc.common.exception.UnauthorizedException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.BeanPropertyUtils;
import com.gnilc.common.utils.PageResult;
import com.gnilc.core.admin.cache.AdminCacheService;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.core.admin.dao.AdminDao;
import com.gnilc.core.session.AdminSessionManager;
import com.gnilc.core.session.SessionTokenPair;
import com.gnilc.core.admin.entity.bo.AdminBo;
import com.gnilc.core.admin.entity.dto.AdminDto;
import com.gnilc.core.admin.entity.dto.AdminPageDto;
import com.gnilc.core.admin.entity.dto.AdminRoleDto;
import com.gnilc.core.admin.entity.vo.AdminTokenVo;
import com.gnilc.core.admin.entity.vo.AdminVo;
import com.gnilc.core.admin.event.AdminEvent;
import com.gnilc.core.admin.service.AdminService;
import com.gnilc.core.context.UserContextService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;


/**
 * 编排后台管理员资料、会话和 RBAC 角色。
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminDao, AdminBo> implements AdminService {
    private static final String ADMIN_DEFAULT_ROLE_CODE = "admin";
    private static final String DEFAULT_HOME_PATH = "/dashboard";
    private static final int USERNAME_MAX_LENGTH = 255;
    private static final int NICKNAME_MAX_LENGTH = 255;
    private static final int PROFILE_TEXT_MAX_LENGTH = 500;
    private static final int HOME_PATH_MAX_LENGTH = 500;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Pattern TEXT_VALUE_PATTERN = Pattern.compile("\\S(?:.*\\S)?");
    private static final Pattern TOKEN_VALUE_PATTERN = Pattern.compile("\\S+");

    private final AdminSessionManager sessionManager;
    private final AdminCacheService adminCacheService;
    private final RoleService roleService;
    private final MenuService menuService;
    private final RoleMenuService roleMenuService;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserContextService userContextService;
    private final I18nMessageService i18nMessageService;

    /** 创建 Admin 应用服务。 */
    public AdminServiceImpl(AdminSessionManager sessionManager,
                            AdminCacheService adminCacheService,
                            RoleService roleService,
                            MenuService menuService,
                            RoleMenuService roleMenuService,
                            UserService userService,
                            UserRoleService userRoleService,
                            ApplicationEventPublisher eventPublisher,
                            UserContextService userContextService,
                            I18nMessageService i18nMessageService) {
        this.sessionManager = sessionManager;
        this.adminCacheService = adminCacheService;
        this.roleService = roleService;
        this.menuService = menuService;
        this.roleMenuService = roleMenuService;
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.eventPublisher = eventPublisher;
        this.userContextService = userContextService;
        this.i18nMessageService = i18nMessageService;
    }

    /**
     * 登录管理员并创建令牌。
     */
    @Override
    public AdminTokenVo login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new AuthenticationFailedException(
                    i18nMessageService.get("system.admin.login.invalidCredentials"));
        }
        AdminBo bo = getAdminByUsername(username);
        if (bo == null || Boolean.FALSE.equals(bo.getStatus())) {
            throw new AuthenticationFailedException(
                    i18nMessageService.get("system.admin.login.invalidCredentials"));
        }
        if (!PASSWORD_ENCODER.matches(password, bo.getPassword())) {
            throw new AuthenticationFailedException(
                    i18nMessageService.get("system.admin.login.invalidCredentials"));
        }
        SessionTokenPair pair = sessionManager.createSession(bo.getUserId());
        return AdminTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    /**
     * 刷新访问令牌。
     */
    @Override
    public AdminTokenVo refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new UnauthorizedException(i18nMessageService.get("system.auth.session.expired"));
        }
        SessionTokenPair pair = sessionManager.refreshSession(refreshToken);
        if (pair == null) {
            throw new UnauthorizedException(i18nMessageService.get("system.auth.session.expired"));
        }
        return AdminTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    /**
     * 登出当前会话。
     */
    @Override
    public void logout(String refreshToken) {
        if (StringUtils.isBlank(refreshToken) || !sessionManager.logout(refreshToken)) {
            throw new UnauthorizedException(i18nMessageService.get("system.auth.unauthorized"));
        }
    }

    /**
     * 查询当前管理员资料。
     */
    @Override
    public AdminVo getUserInfo() {
        Long userId = userContextService.getUserId();
        AdminVo vo = adminCacheService.getUserInfo(userId, () -> {
            AdminBo bo = getAdminByUserId(userId);
            if (bo == null) {
                return null;
            }
            AdminVo userInfo = new AdminVo();
            BeanUtils.copyProperties(bo, userInfo);
            userInfo.setDesc(bo.getDescription());
            userInfo.setStatus(null);
            return userInfo;
        });
        if (vo == null) {
            return null;
        }
        vo.setRoleCodes(getRoleCodes(userId));
        return vo;
    }

    /** 更新当前 Admin Profile。 */
    @Override
    @Transactional
    public void updateProfile(AdminDto dto) {
        Preconditions.checkArgument(dto != null, i18nMessageService.get("system.admin.profile.required"));
        String nickname = dto.getNickname();
        Preconditions.checkArgument(StringUtils.isNotEmpty(nickname),
                i18nMessageService.get("system.admin.nickname.required"));
        Preconditions.checkArgument(TEXT_VALUE_PATTERN.matcher(nickname).matches(),
                i18nMessageService.get("system.admin.nickname.invalid"));
        Preconditions.checkArgument(nickname.codePointCount(0, nickname.length()) <= NICKNAME_MAX_LENGTH,
                i18nMessageService.get("system.admin.nickname.tooLong", NICKNAME_MAX_LENGTH));
        String avatar = dto.getAvatar();
        validateOptionalText(avatar, TOKEN_VALUE_PATTERN, PROFILE_TEXT_MAX_LENGTH,
                "system.admin.avatar.invalid", "system.admin.avatar.tooLong");
        String description = dto.getDesc();
        validateOptionalText(description, TEXT_VALUE_PATTERN, PROFILE_TEXT_MAX_LENGTH,
                "system.admin.description.invalid", "system.admin.description.tooLong");

        AdminBo bo = getAdminByUserId(userContextService.getUserId());
        Preconditions.checkCondition(bo != null,
                i18nMessageService.get("system.admin.notFound.signIn"));
        lambdaUpdate()
                .set(AdminBo::getNickname, nickname)
                .set(AdminBo::getAvatar, avatar)
                .set(AdminBo::getDescription, description)
                .eq(AdminBo::getId, bo.getId())
                .update();
        eventPublisher.publishEvent(new AdminEvent(AdminEvent.Action.UPDATE, bo.getUserId()));
    }

    /** 修改当前 Admin 登录密码。 */
    @Override
    @Transactional
    public void updatePassword(String oldPassword, String newPassword) {
        Preconditions.checkArgument(StringUtils.isNotBlank(oldPassword),
                i18nMessageService.get("system.admin.password.current.required"));
        validateStrongPassword(newPassword);

        Long userId = userContextService.getUserId();
        AdminBo bo = getAdminByUserId(userId);
        Preconditions.checkCondition(bo != null,
                i18nMessageService.get("system.admin.notFound.signIn"));
        Preconditions.checkArgument(PASSWORD_ENCODER.matches(oldPassword, bo.getPassword()),
                i18nMessageService.get("system.admin.password.current.incorrect"));

        lambdaUpdate()
                .set(AdminBo::getPassword, PASSWORD_ENCODER.encode(newPassword))
                .eq(AdminBo::getId, bo.getId())
                .update();
        sessionManager.cleanupUserSessions(userId);
    }

    /**
     * 查询当前管理员角色标识。
     */
    @Override
    public List<String> getRoleCodes() {
        return getRoleCodes(userContextService.getUserId());
    }

    /**
     * 查询当前管理员按钮访问标识。
     */
    @Override
    public List<String> getMenuAccessCodes() {
        return getMenuAccessCodes(userContextService.getUserId());
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
        return adminCacheService.getRoleCodes(userId,
                () -> Optional.ofNullable(roleService.getRoles(userId)).orElse(List.of()).stream()
                        .map(RoleBo::getCode)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .toList());
    }

    /**
     * 查询用户按钮访问标识。
     */
    @Override
    public List<String> getMenuAccessCodes(Long userId) {
        return adminCacheService.getMenuAccessCodes(userId,
                () -> Optional.ofNullable(userService.getMenus(userId)).orElse(List.of()).stream()
                        .filter(menu -> menu.getType() == MenuType.BUTTON)
                        .filter(MenuBo::getStatus)
                        .map(MenuBo::getAccessCode)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .distinct()
                        .toList());
    }

    /** 查询当前 Admin 可访问的菜单路由。 */
    @Override
    public List<MenuRouteVo> getMenuRoutes() {
        Long userId = userContextService.getUserId();
        return adminCacheService.getMenuRoutes(userId, () -> {
            List<Long> roleIds = userRoleService.getRoleIds(userId);
            List<Long> menuIds = roleMenuService.getMenuIds(roleIds);
            return menuService.getMenuRoutes(menuIds);
        });
    }

    /**
     * 创建管理员。
     */
    @Override
    @Transactional
    public void createAdmin(AdminDto dto) {
        validateAdmin(dto, false);
        String username = dto.getUsername();
        String password = dto.getPassword();
        Long userId = userService.createUser();
        AdminBo bo = new AdminBo();
        bo.setUserId(userId);
        bo.setUsername(username);
        bo.setPassword(PASSWORD_ENCODER.encode(password));
        bo.setNickname(dto.getNickname());
        bo.setAvatar(dto.getAvatar());
        bo.setDescription(dto.getDesc());
        bo.setHomePath(dto.getHomePath() == null ? DEFAULT_HOME_PATH : dto.getHomePath());
        bo.setStatus(dto.getStatus());
        save(bo);
        replaceAdminRoles(userId, dto.getRoleCodes());
    }

    /**
     * 更新管理员资料。
     */
    @Override
    @Transactional
    public void updateAdmin(AdminDto dto) {
        AdminBo bo = validateAdmin(dto, true);
        boolean wasEnabled = Boolean.TRUE.equals(bo.getStatus())
                && Boolean.FALSE.equals(dto.getStatus());
        BeanPropertyUtils.copyNonNullProperties(dto, bo);
        if (dto.isAvatarSpecified()) {
            bo.setAvatar(dto.getAvatar());
        }
        if (dto.isDescSpecified()) {
            bo.setDescription(dto.getDesc());
        }

        // 单独处理密码。
        String password = dto.getPassword();
        if (password != null) {
            bo.setPassword(PASSWORD_ENCODER.encode(password));
        } else {
            bo.setPassword(null);
        }

        updateById(bo);
        saveRolesIfProvided(bo.getUserId(), dto.getRoleCodes());
        eventPublisher.publishEvent(new AdminEvent(AdminEvent.Action.UPDATE, bo.getUserId()));

        if (wasEnabled) {
            sessionManager.cleanupUserSessions(bo.getUserId());
        }
    }

    /**
     * 保存管理员角色。
     */
    @Override
    @Transactional
    public void saveAdminRoles(AdminRoleDto dto) {
        AdminBo bo = getById(dto.getId());
        Preconditions.checkCondition(bo != null, i18nMessageService.get("system.admin.notFound"));
        replaceAdminRoles(bo.getUserId(), dto.getRoleCodes());
    }

    /**
     * 删除管理员。
     */
    @Override
    @Transactional
    public void removeAdmin(Long id) {
        Preconditions.checkArgument(id != null, i18nMessageService.get("system.admin.selection.required"));
        AdminBo bo = getById(id);
        Preconditions.checkCondition(bo != null, i18nMessageService.get("system.admin.notFound"));
        Preconditions.checkCondition(!Objects.equals(bo.getUserId(), userContextService.getUserId()),
                i18nMessageService.get("system.admin.current.delete"));
        sessionManager.cleanupUserSessions(bo.getUserId());
        bo.setUsername(bo.getUsername() + "_del_" + id);
        updateById(bo);
        removeById(id);
        userService.removeUser(bo.getUserId());
        replaceRoles(bo.getUserId(), List.of());
        eventPublisher.publishEvent(new AdminEvent(AdminEvent.Action.DELETE, bo.getUserId()));
    }

    /**
     * 分页查询管理员。
     */
    @Override
    public PageResult<AdminVo> getAdminPage(AdminPageDto params) {
        Preconditions.checkArgument(params != null,
                i18nMessageService.get("system.admin.information.required"));
        String username = params.getUsername();
        String nickname = params.getNickname();
        boolean usernameSpecified = StringUtils.isNotBlank(username);
        boolean nicknameSpecified = StringUtils.isNotBlank(nickname);
        if (usernameSpecified) {
            validateOptionalText(username, TOKEN_VALUE_PATTERN, USERNAME_MAX_LENGTH,
                    "system.admin.username.invalid", "system.admin.username.tooLong");
        }
        if (nicknameSpecified) {
            validateOptionalText(nickname, TEXT_VALUE_PATTERN, NICKNAME_MAX_LENGTH,
                    "system.admin.nickname.invalid", "system.admin.nickname.tooLong");
        }
        IPage<AdminBo> page = lambdaQuery()
                .eq(usernameSpecified, AdminBo::getUsername, username)
                .like(nicknameSpecified, AdminBo::getNickname, nickname)
                .eq(params.getStatus() != null, AdminBo::getStatus, params.getStatus())
                .orderByDesc(AdminBo::getId)
                .page(params.getPage());
        List<AdminVo> vos = page.getRecords().stream()
                .map(bo -> {
                    AdminVo vo = new AdminVo();
                    BeanUtils.copyProperties(bo, vo);
                    vo.setDesc(bo.getDescription());
                    vo.setRoleCodes(getRoleCodes(bo.getUserId()));
                    return vo;
                })
                .toList();
        return PageResult.of(page, vos);
    }

    /** 按 RBAC User ID 查询 Admin。 */
    @Override
    public AdminBo getAdminByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(AdminBo::getUserId, userId)
                .one();
    }

    /** 按 Admin ID 查询 Admin。 */
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
                i18nMessageService.get("system.admin.password.weak"));
    }

    /** 校验 Admin 创建或更新输入。 */
    private AdminBo validateAdmin(AdminDto dto, boolean update) {
        Preconditions.checkArgument(dto != null, i18nMessageService.get("system.admin.information.required"));
        boolean usernameSpecified = dto.getUsername() != null;
        boolean nicknameSpecified = dto.getNickname() != null;
        AdminBo admin = update ? getAdmin(dto.getId()) : null;
        if (update) {
            Preconditions.checkCondition(admin != null, i18nMessageService.get("system.admin.notFound"));
            Preconditions.checkArgument(!usernameSpecified || StringUtils.isNotEmpty(dto.getUsername()),
                    i18nMessageService.get("system.admin.username.required"));
            Preconditions.checkArgument(!usernameSpecified
                            || TOKEN_VALUE_PATTERN.matcher(dto.getUsername()).matches(),
                    i18nMessageService.get("system.admin.username.invalid"));
            Preconditions.checkArgument(!nicknameSpecified || StringUtils.isNotEmpty(dto.getNickname()),
                    i18nMessageService.get("system.admin.nickname.required"));
            Preconditions.checkArgument(!nicknameSpecified
                            || TEXT_VALUE_PATTERN.matcher(dto.getNickname()).matches(),
                    i18nMessageService.get("system.admin.nickname.invalid"));
        } else {
            Preconditions.checkArgument(StringUtils.isNotEmpty(dto.getUsername()),
                    i18nMessageService.get("system.admin.username.required"));
            Preconditions.checkArgument(TOKEN_VALUE_PATTERN.matcher(dto.getUsername()).matches(),
                    i18nMessageService.get("system.admin.username.invalid"));
            Preconditions.checkArgument(StringUtils.isNotBlank(dto.getPassword()),
                    i18nMessageService.get("system.admin.password.required"));
            Preconditions.checkArgument(StringUtils.isNotEmpty(dto.getNickname()),
                    i18nMessageService.get("system.admin.nickname.required"));
            Preconditions.checkArgument(TEXT_VALUE_PATTERN.matcher(dto.getNickname()).matches(),
                    i18nMessageService.get("system.admin.nickname.invalid"));
        }
        Preconditions.checkArgument(dto.getUsername() == null
                        || dto.getUsername().codePointCount(0, dto.getUsername().length()) <= USERNAME_MAX_LENGTH,
                i18nMessageService.get("system.admin.username.tooLong", USERNAME_MAX_LENGTH));
        Preconditions.checkArgument(dto.getNickname() == null
                        || dto.getNickname().codePointCount(0, dto.getNickname().length()) <= NICKNAME_MAX_LENGTH,
                i18nMessageService.get("system.admin.nickname.tooLong", NICKNAME_MAX_LENGTH));
        validateOptionalText(dto.getAvatar(), TOKEN_VALUE_PATTERN, PROFILE_TEXT_MAX_LENGTH,
                "system.admin.avatar.invalid", "system.admin.avatar.tooLong");
        validateOptionalText(dto.getDesc(), TEXT_VALUE_PATTERN, PROFILE_TEXT_MAX_LENGTH,
                "system.admin.description.invalid", "system.admin.description.tooLong");
        validateOptionalText(dto.getHomePath(), TOKEN_VALUE_PATTERN, HOME_PATH_MAX_LENGTH,
                "system.admin.homePath.invalid", "system.admin.homePath.tooLong");
        String username = dto.getUsername();
        if (!update || username != null && !username.equals(admin.getUsername())) {
            Preconditions.checkArgument(getAdminByUsername(username) == null,
                    i18nMessageService.get("system.admin.username.exists"));
        }
        if (update) {
            boolean disablesCurrentAdmin = Boolean.TRUE.equals(admin.getStatus())
                    && Boolean.FALSE.equals(dto.getStatus())
                    && Objects.equals(admin.getUserId(), userContextService.getUserId());
            Preconditions.checkCondition(!disablesCurrentAdmin, i18nMessageService.get("system.admin.current.disable"));
        }
        if (!update || dto.getPassword() != null) {
            validateStrongPassword(dto.getPassword());
        }
        return admin;
    }

    /** 校验可选文本原值的格式和 Unicode 长度。 */
    private void validateOptionalText(
            String value,
            Pattern format,
            int maxLength,
            String invalidMessageKey,
            String tooLongMessageKey) {
        Preconditions.checkArgument(value == null || StringUtils.isNotEmpty(value)
                        && format.matcher(value).matches(),
                i18nMessageService.get(invalidMessageKey));
        Preconditions.checkArgument(value == null
                        || value.codePointCount(0, value.length()) <= maxLength,
                i18nMessageService.get(tooLongMessageKey, maxLength));
    }

    /**
     * 替换用户角色。
     */
    private void saveRolesIfProvided(Long userId, List<String> roleCodes) {
        if (roleCodes == null) {
            return;
        }
        replaceAdminRoles(userId, roleCodes);
    }

    /** 合并默认 Admin 角色并替换用户角色。 */
    private void replaceAdminRoles(Long userId, List<String> roleCodes) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        codes.add(ADMIN_DEFAULT_ROLE_CODE);
        if (roleCodes != null) {
            codes.addAll(roleCodes);
        }
        replaceRoles(userId, codes.stream().toList());
    }

    /** 将角色编码解析为角色 ID 并替换用户角色。 */
    private void replaceRoles(Long userId, List<String> roleCodes) {
        List<String> codes = roleCodes == null ? List.of() : roleCodes;
        List<Long> roleIds = codes.stream()
                .map(code -> {
                    Preconditions.checkArgument(StringUtils.isNotBlank(code), i18nMessageService.get("rbac.role.code.required"));
                    RoleBo bo = roleService.getRoleByCode(code);
                    Preconditions.checkCondition(bo != null,
                            i18nMessageService.get("rbac.role.notFound"));
                    return bo.getId();
                })
                .toList();
        UserRoleDto dto = new UserRoleDto();
        dto.setUserId(userId);
        dto.setRoleIds(roleIds);
        userRoleService.updateUserRole(dto);
    }

}
