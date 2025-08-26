// ===== 공통 LocalStorage 키 =====
const LS = {
    access:  'jwt_access_token',
    refresh: 'jwt_refresh_token',
    member:  'member_info'
};

// ===== 카카오 SDK 초기화 =====
function initKakao() {
    return new Promise((resolve, reject) => {
        // meta 태그에서 카카오 JS Key 가져오기
        const key = document.querySelector('meta[name="kakao-js-key"]')?.content;

        if (!key || key.trim() === '') {
            reject(new Error('카카오 JS Key가 설정되지 않았습니다. 서버 환경변수를 확인해주세요.'));
            return;
        }

        console.log('카카오 JS Key 확인됨:', key.substring(0, 10) + '...');

        // 카카오 SDK가 로드될 때까지 기다림
        const waitForKakao = () => {
            if (window.Kakao) {
                try {
                    if (!window.Kakao.isInitialized()) {
                        window.Kakao.init(key);
                        console.log('카카오 SDK 초기화 완료');
                    }
                    resolve();
                } catch (error) {
                    reject(new Error('카카오 SDK 초기화 실패: ' + error.message));
                }
            } else {
                // 최대 5초 대기
                if (Date.now() - startTime > 5000) {
                    reject(new Error('카카오 SDK 로드 시간 초과'));
                    return;
                }
                setTimeout(waitForKakao, 100);
            }
        };

        const startTime = Date.now();
        waitForKakao();
    });
}

// ===== 카카오 로그인 및 토큰 교환 =====
async function kakaoLoginAndExchange() {
    return new Promise((resolve, reject) => {
        if (!window.Kakao || !window.Kakao.Auth) {
            return reject(new Error('카카오 SDK가 초기화되지 않았습니다.'));
        }

        console.log('카카오 로그인 시작...');

        window.Kakao.Auth.login({
            scope: 'profile_nickname,profile_image,account_email',
            success: async (authObj) => {
                console.log('카카오 인증 성공:', authObj);

                try {
                    const providerAccessToken = authObj.access_token;
                    console.log('서버로 토큰 교환 요청 시작...');

                    // 서버에 카카오 토큰을 보내서 JWT 토큰 받기
                    const res = await fetch('/api/v1/auth/login/kakao', {
                        method: 'POST',
                        headers: { 'Content-Type': 'text/plain' },
                        body: providerAccessToken
                    });

                    console.log('서버 응답 상태:', res.status);

                    if (!res.ok) {
                        const errorText = await res.text();
                        console.error('서버 응답 에러:', errorText);
                        throw new Error(`로그인 실패: ${res.status} - ${errorText}`);
                    }

                    const data = await res.json();
                    console.log('서버 응답 데이터:', data);

                    // LoginResponse 구조에 맞게 파싱
                    const tokenInfo = data.tokenInfo || data;
                    const memberInfo = data.memberInfo || data.member;

                    const accessToken = tokenInfo.accessToken || tokenInfo.access_token;
                    const refreshToken = tokenInfo.refreshToken || tokenInfo.refresh_token;

                    if (!accessToken) {
                        throw new Error('서버에서 accessToken을 받지 못했습니다.');
                    }

                    // 토큰 저장
                    localStorage.setItem(LS.access, accessToken);
                    if (refreshToken) {
                        localStorage.setItem(LS.refresh, refreshToken);
                    }
                    if (memberInfo) {
                        localStorage.setItem(LS.member, JSON.stringify(memberInfo));
                    }

                    console.log('로그인 성공, 토큰 저장 완료');
                    resolve();
                } catch (error) {
                    console.error('토큰 교환 중 오류:', error);
                    reject(error);
                }
            },
            fail: (err) => {
                console.error('카카오 로그인 실패:', err);
                reject(new Error('카카오 로그인에 실패했습니다: ' + JSON.stringify(err)));
            }
        });
    });
}

// ===== 프로필 API 호출 =====
async function loadProfile() {
    const access = localStorage.getItem(LS.access);
    if (!access) {
        throw new Error('로그인이 필요합니다.');
    }

    console.log('프로필 정보 요청...');

    const res = await fetch('/api/user/profile', {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${access}` }
    });

    if (!res.ok) {
        if (res.status === 401) {
            // 토큰 만료
            localStorage.clear();
            throw new Error('로그인이 만료되었습니다. 다시 로그인해주세요.');
        }
        throw new Error(`프로필 조회 실패: ${res.status}`);
    }

    return res.json();
}

// ===== 로그아웃 =====
async function logout() {
    const refresh = localStorage.getItem(LS.refresh);

    try {
        await fetch('/api/v1/auth/logout', {
            method: 'POST',
            headers: refresh ? { 'Refresh-Token': refresh } : {}
        });
    } catch (error) {
        console.error('로그아웃 API 호출 실패:', error);
    }

    // 로컬 스토리지 정리
    localStorage.removeItem(LS.access);
    localStorage.removeItem(LS.refresh);
    localStorage.removeItem(LS.member);

    console.log('로그아웃 완료');
}

// ===== 페이지별 초기화 =====
document.addEventListener('DOMContentLoaded', async () => {
    const path = location.pathname;
    console.log('현재 페이지:', path);

    // 로그인 페이지
    if (path === '/' || path === '/login') {
        const btn = document.getElementById('kakaoLoginBtn');
        const errorBox = document.getElementById('errorBox');

        if (btn && errorBox) {
            // 카카오 SDK 초기화
            try {
                await initKakao();
                console.log('카카오 SDK 준비 완료');
            } catch (error) {
                console.error('카카오 SDK 초기화 실패:', error);
                errorBox.textContent = error.message;
                errorBox.style.display = 'block';
                return;
            }

            btn.addEventListener('click', async () => {
                console.log('로그인 버튼 클릭');
                errorBox.style.display = 'none';
                btn.disabled = true;
                btn.textContent = '로그인 중...';

                try {
                    await kakaoLoginAndExchange();
                    console.log('로그인 성공, 프로필 페이지로 이동');
                    location.href = '/profile';
                } catch (error) {
                    console.error('로그인 실패:', error);
                    errorBox.textContent = error.message || '로그인 중 오류가 발생했습니다.';
                    errorBox.style.display = 'block';
                } finally {
                    btn.disabled = false;
                    btn.textContent = '카카오로 로그인';
                }
            });
        }
    }

    // 프로필 페이지
    if (path === '/profile') {
        const toHomeBtn = document.getElementById('toHomeBtn');
        const logoutBtn = document.getElementById('logoutBtn');
        const errorBox = document.getElementById('errorBox');

        if (toHomeBtn) {
            toHomeBtn.addEventListener('click', () => location.href = '/login');
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', async () => {
                try {
                    await logout();
                } finally {
                    location.href = '/login';
                }
            });
        }

        // 프로필 정보 로드
        try {
            const profile = await loadProfile();
            console.log('프로필 정보:', profile);

            // UI 업데이트
            const nickname = profile.nickname || profile.name || profile.username || '사용자';
            const email = profile.email || '—';
            const provider = profile.providerType || profile.provider || '—';
            const role = profile.role || profile.memberRole || '—';
            const memberId = profile.id || profile.memberId || '—';

            document.getElementById('nickname').textContent = nickname;
            document.getElementById('email').textContent = email;
            document.getElementById('kv-nickname').textContent = nickname;
            document.getElementById('kv-email').textContent = email;
            document.getElementById('kv-provider').textContent = provider;
            document.getElementById('kv-role').textContent = role;
            document.getElementById('kv-id').textContent = memberId;

            // 프로필 이미지
            const avatarUrl = profile.profileImage || profile.imageUrl || profile.avatarUrl;
            if (avatarUrl) {
                document.getElementById('avatar').src = avatarUrl;
            }

            // 원본 JSON
            document.getElementById('rawJson').textContent = JSON.stringify(profile, null, 2);
        } catch (error) {
            console.error('프로필 로드 실패:', error);
            if (errorBox) {
                errorBox.textContent = error.message;
                errorBox.style.display = 'block';
            }
            // 로그인 페이지로 리다이렉트
            setTimeout(() => location.href = '/login', 1500);
        }
    }
});