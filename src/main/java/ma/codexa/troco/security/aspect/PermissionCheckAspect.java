package ma.codexa.troco.security.aspect;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.security.service.PermissionCheckService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionCheckAspect {

    private final PermissionCheckService permissionCheckService;

    @Around("@annotation(ma.codexa.troco.security.annotations.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        String permissionName = requirePermission.value();

        if (!permissionCheckService.currentUserHasPermission(permissionName)) {
            throw new BusinessException(
                    "Vous n'avez pas la permission nécessaire : " + permissionName,
                    HttpStatus.FORBIDDEN);
        }
        return joinPoint.proceed();
    }
}
