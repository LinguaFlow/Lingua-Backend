// ===== 공통 LocalStorage 키 =====
const LS = {
    access:  'jwt_access_token',
    refresh: 'jwt_refresh_token',
    member:  'member_info' // 선택 저장
};

// ===== 카카오 SDK 초기화 & 로그인 플로우 =====
function initKakao() {
    const key = window.KAKAO_JS_KEY || document.querySelector('meta[name="kakao-js-key"]')?.content;
    if (!key) {
        console.warn('Kakao JS Key가 설정되지 않았습니다. window.KAKAO_JS_KEY에 키를 넣어주세요.');
        return;
    }
    if (!window.Kakao?.isInitialized?.()) {
        window.Kakao.init(key);
    }
}

async function kakaoLoginAndExchange() {
    return new Promise((resolve, reject) => {
        if (!window.Kakao) return reject(new Error('Kakao SDK 미로딩'));
        window.Kakao.Auth.login({
            scope: 'profile_nickname, profile_image, account_email',
            success: async (authObj) => {
                try {
                    const providerAccessToken = authObj.access_token; // 클라이언트용 카카오 액세스 토큰
                    // 서버에 교환 요청 (본문: text/plain)
                    const res = await fetch('/api/v1/auth/login/kakao', {
                        method: 'POST',
                        headers: { 'Content-Type': 'text/plain' },
                        body: providerAccessToken
                    });
                    if (!res.ok) {
                        const txt = await res.text();
                        throw new Error(`로그인 실패: ${res.status} ${txt}`);
                    }
                    const data = await res.json();
                    // TokenInfo 추정 필드 매핑(서버 DTO에 맞게 조정)
                    const tokenInfo = data.tokenInfo || data.token || data;
                    const accessToken  = tokenInfo.accessToken  || tokenInfo.access_token;
                    const refreshToken = tokenInfo.refreshToken || tokenInfo.refresh_token;

                    if (!accessToken) throw new Error('서버에서 accessToken을 찾을 수 없습니다.');

                    // 저장
                    localStorage.setItem(LS.access, accessToken);
                    if (refreshToken) localStorage.setItem(LS.refresh, refreshToken);
                    if (data.memberInfo) localStorage.setItem(LS.member, JSON.stringify(data.memberInfo));

                    resolve();
                } catch (e) { reject(e); }
            },
            fail: (err) => reject(err)
        });
    });
}

// ===== 프로필 API 호출 =====
async function loadProfile() {
    const access = localStorage.getItem(LS.access);
    if (!access) throw new Error('로그인이 필요합니다.');

    const res = await fetch('/api/user/profile', {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${access}` }
    });
    if (!res.ok) throw new Error(`프로필 조회 실패: ${res.status}`);
    return res.json();
}

// ===== 로그아웃 =====
async function logout() {
    const refresh = localStorage.getItem(LS.refresh);
    // 서버는 Refresh-Token 헤더로 로그아웃 처리
    await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: refresh ? { 'Refresh-Token': refresh } : {}
    });
    // 로컬 삭제
    localStorage.removeItem(LS.access);
    localStorage.removeItem(LS.refresh);
    localStorage.removeItem(LS.member);
}

// ===== 페이지별 초기화 =====
document.addEventListener('DOMContentLoaded', async () => {
    const path = location.pathname;

    // 로그인 페이지
    if (path === '/' || path === '/login') {
        initKakao();
        const btn = document.getElementById('kakaoLoginBtn');
        const errorBox = document.getElementById('errorBox');
        if (btn) {
            btn.addEventListener('click', async () => {
                errorBox.style.display = 'none';
                try {
                    await kakaoLoginAndExchange(); // 카카오 토큰 → 서버 로그인(JWT)
                    location.href = '/profile';
                } catch (e) {
                    console.error(e);
                    errorBox.textContent = e?.message || '로그인 중 오류가 발생했습니다.';
                    errorBox.style.display = 'block';
                }
            });
        }
    }

    // 프로필 페이지
    if (path === '/profile') {
        const toHomeBtn = document.getElementById('toHomeBtn');
        const logoutBtn = document.getElementById('logoutBtn');
        const err = document.getElementById('errorBox');

        if (toHomeBtn) toHomeBtn.addEventListener('click', () => location.href = '/');

        if (logoutBtn) {
            logoutBtn.addEventListener('click', async () => {
                try {
                    await logout();
                } finally {
                    location.href = '/login';
                }
            });
        }

        try {
            const profile = await loadProfile();
            // 예상 가능한 필드 우선 표시(없으면 대체)
            const nickname = profile.nickname || profile.name || profile.username || '사용자';
            const email    = profile.email || '—';
            const provider = profile.providerType || profile.provider || '—';
            const role     = profile.role || profile.memberRole || '—';
            const id       = profile.id || profile.memberId || '—';

            document.getElementById('nickname').textContent   = nickname;
            document.getElementById('email').textContent      = email;
            document.getElementById('kv-nickname').textContent= nickname;
            document.getElementById('kv-email').textContent   = email;
            document.getElementById('kv-provider').textContent= provider;
            document.getElementById('kv-role').textContent    = role;
            document.getElementById('kv-id').textContent      = id;

            // 멤버 속에 profileImage 같은 필드가 있으면 적용
            const avatarUrl = profile.profileImage || profile.imageUrl || profile.avatarUrl || null;
            if (avatarUrl) document.getElementById('avatar').src = avatarUrl;

            // 원본 JSON 출력
            document.getElementById('rawJson').textContent = JSON.stringify(profile, null, 2);
        } catch (e) {
            console.error(e);
            if (err) {
                err.textContent = e?.message || '프로필을 불러오는 중 오류가 발생했습니다.';
                err.style.display = 'block';
            }
            // 토큰 만료 시 로그인으로
            setTimeout(() => location.href = '/login', 800);
        }
    }
});
