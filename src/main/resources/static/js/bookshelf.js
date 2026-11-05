// 책장(읽은 책) 캘린더 + 추가/수정/삭제 로직

let bsReading = [];      // 읽는 중 (finishedAt == null)
let bsFinished = [];     // 완독 (finishedAt != null)
let bsFinishedByDate = {}; // 'YYYY-MM-DD' -> [entry, ...]
let bsIsOwner = false;

let calYear = 0;         // 현재 표시 중인 달력 연/월(0-based)
let calMonth = 0;

let bsSelectedBook = null;   // 추가 모달에서 선택한 책
let bsEditingEntry = null;   // 상세/편집 모달에서 다루는 항목

let bsCurrentView = 'calendar'; // 'calendar' | 'shelf'
let bsResizeTimer = null;
const BS_VIEW_KEY = 'bookview.shelfView';

document.addEventListener('DOMContentLoaded', () => {
    const panel = document.getElementById('panelShelf');
    if (!panel) return;
    bsIsOwner = panel.dataset.owner === 'true';

    bsReading = parseDataIsland('bookshelfReadingData');
    bsFinished = parseDataIsland('bookshelfFinishedData');

    bsFinishedByDate = {};
    bsFinished.forEach(e => {
        if (!e.finishedAt) return;
        (bsFinishedByDate[e.finishedAt] = bsFinishedByDate[e.finishedAt] || []).push(e);
    });

    // 달력 시작 위치: 가장 최근 완독 달, 없으면 이번 달
    const anchor = bsFinished.length > 0 && bsFinished[0].finishedAt
        ? new Date(bsFinished[0].finishedAt)
        : new Date();
    calYear = anchor.getFullYear();
    calMonth = anchor.getMonth();

    Modal.setupOutsideClick('addBookModal');
    Modal.setupOutsideClick('entryDetailModal');
    Modal.setupOutsideClick('dayBooksModal');

    renderReadingStats(parseDataIsland('readingStatsData'));
    renderShelf();
    window.addEventListener('resize', bsOnResize);
    bindShelfControls();

    // 저장/수정/삭제 후 리로드해도 '읽은 책' 탭에 머물도록 복원
    if (location.hash === '#shelf') switchBookshelfTab('shelf');
});

function bindShelfControls() {
    const search = document.getElementById('bsBookSearch');
    if (search) {
        search.addEventListener('keydown', (event) => {
            if (event.key !== 'Enter') return;
            event.preventDefault();
            bsSearchBooks();
        });
    }

    document.addEventListener('click', (event) => {
        const trigger = event.target.closest('[data-action]');
        if (!trigger) return;

        switch (trigger.dataset.action) {
            case 'bs-tab':
                switchBookshelfTab(trigger.dataset.tab);
                break;
            case 'bs-view':
                setShelfView(trigger.dataset.view);
                break;
            case 'bs-month':
                bookshelfChangeMonth(Number(trigger.dataset.delta));
                break;
            case 'bs-add-open':
                openAddBookModal();
                break;
            case 'bs-add-close':
                closeAddBookModal();
                break;
            case 'bs-search':
                bsSearchBooks();
                break;
            case 'bs-clear-selection':
                bsClearSelection();
                break;
            case 'bs-submit':
                bsSubmitEntry();
                break;
            case 'bs-detail-close':
                closeEntryDetailModal();
                break;
            case 'bs-entry-delete':
                bsDeleteEntry();
                break;
            case 'bs-entry-save':
                bsSaveEntryEdit();
                break;
            case 'bs-day-close':
                closeDayModal();
                break;
        }
    });
}

function bsOnResize() {
    clearTimeout(bsResizeTimer);
    bsResizeTimer = setTimeout(() => {
        if (bsCurrentView === 'shelf') renderShelfView();
    }, 200);
}

// 같은 책(bookIsbn)의 재독 순번. renderShelf/bsRerender에서 매번 다시 계산한다.
// - orderMap: 완독 항목 id -> 완독일 오름차순 회차(1부터)
// - countByIsbn: isbn -> 완독 총 횟수 (읽는 중 항목의 "N번째 읽는 중" 안내에 사용)
let bsReadOrder = { orderMap: {}, countByIsbn: {} };

function buildReadOrderMaps() {
    const byIsbn = {};
    bsFinished.forEach(e => {
        if (!e.bookIsbn) return;
        (byIsbn[e.bookIsbn] = byIsbn[e.bookIsbn] || []).push(e);
    });
    const orderMap = {};
    const countByIsbn = {};
    Object.keys(byIsbn).forEach(isbn => {
        const list = byIsbn[isbn].slice().sort((a, b) => (a.finishedAt || '').localeCompare(b.finishedAt || ''));
        countByIsbn[isbn] = list.length;
        list.forEach((e, idx) => { orderMap[e.id] = idx + 1; });
    });
    bsReadOrder = { orderMap, countByIsbn };
}

// 책장/모달 등에 노출하는 정식 문구. 1회차(재독 아님)는 노출할 정보가 없으므로 null.
// - 완독 항목: "2회차 읽음"
// - 읽는 중 항목: 같은 책의 완독 이력이 있으면 "2번째 읽는 중" (확정 회차 아님)
function readTag(entry) {
    if (!entry.bookIsbn) return null;
    if (entry.finishedAt) {
        const n = bsReadOrder.orderMap[entry.id];
        return (n && n >= 2) ? `${n}회차 읽음` : null;
    }
    const total = bsReadOrder.countByIsbn[entry.bookIsbn] || 0;
    return total >= 1 ? `${total + 1}번째 읽는 중` : null;
}

// 책장 표지 하단 띠에 쓰는 축약형 ("2회독" / "3회독 중"). 첫 독서는 표시하지 않으므로 null.
function readBand(entry) {
    if (!entry.bookIsbn) return null;
    if (entry.finishedAt) {
        const n = bsReadOrder.orderMap[entry.id];
        return (n && n >= 2) ? `${n}회독` : null;
    }
    const total = bsReadOrder.countByIsbn[entry.bookIsbn] || 0;
    return total >= 1 ? `${total + 1}회독 중` : null;
}

// 서버 응답(BookshelfEntry) → 클라이언트 표시용 항목
function toClientEntry(e) {
    return {
        id: e.id,
        bookTitle: e.bookTitle,
        bookAuthor: e.bookAuthor,
        bookThumbnail: e.bookThumbnail,
        bookIsbn: e.bookIsbn,
        startedAt: e.startedAt,
        finishedAt: e.finishedAt,
    };
}

// 완독=완독일 최신순, 읽는 중=시작일 최신순 (서버 정렬과 동일하게 유지)
function bsSortEntries() {
    bsFinished.sort((a, b) => (b.finishedAt || '').localeCompare(a.finishedAt || ''));
    bsReading.sort((a, b) => (b.startedAt || '').localeCompare(a.startedAt || ''));
}

// 새로고침 없이 현재 뷰를 다시 그린다 (추가/수정/삭제 후)
function bsRerender() {
    bsFinishedByDate = {};
    bsFinished.forEach(e => {
        if (!e.finishedAt) return;
        (bsFinishedByDate[e.finishedAt] = bsFinishedByDate[e.finishedAt] || []).push(e);
    });
    buildReadOrderMaps();

    const total = bsReading.length + bsFinished.length;
    const empty = document.getElementById('shelfEmpty');
    const viewCalendar = document.getElementById('viewCalendar');
    const viewShelf = document.getElementById('viewShelf');
    const seg = document.querySelector('#panelShelf .bs-seg');

    if (total === 0) {
        empty.classList.remove('hidden');
        viewCalendar.classList.add('hidden');
        viewShelf.classList.add('hidden');
        if (seg) seg.classList.add('hidden');
        return;
    }
    empty.classList.add('hidden');
    if (seg) seg.classList.remove('hidden');
    document.querySelectorAll('#panelShelf .bs-seg-btn').forEach(b => {
        b.classList.toggle('on', b.dataset.view === bsCurrentView);
    });

    renderReadingStrip();
    renderCalendar();
    const isShelf = bsCurrentView === 'shelf';
    viewCalendar.classList.toggle('hidden', isShelf);
    viewShelf.classList.toggle('hidden', !isShelf);
    if (isShelf) renderShelfView();
}

function parseDataIsland(id) {
    const el = document.getElementById(id);
    if (!el) return [];
    try {
        return JSON.parse(el.textContent) || [];
    } catch {
        return [];
    }
}

// ── 탭 전환 ────────────────────────────────────────────────
function switchBookshelfTab(tab) {
    const isReviews = tab === 'reviews';
    // 패널 표시는 루트 클래스(인라인 CSS)로 제어 → Tailwind 비동기 로딩과 무관하게 즉시 반영
    document.documentElement.classList.toggle('__view-shelf', !isReviews);

    setTabActive('tabReviewsBtn', isReviews);
    setTabActive('tabShelfBtn', !isReviews);

    // 현재 탭을 해시에 기록 (새로고침 시 유지)
    history.replaceState(null, '', isReviews ? '#reviews' : '#shelf');

    // 책장 뷰가 활성 상태로 패널이 보이면 (너비 확보) 다시 렌더
    if (!isReviews && bsCurrentView === 'shelf') renderShelfView();
}

function setTabActive(btnId, active) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.classList.toggle('text-stone-800', active);
    btn.classList.toggle('border-amber-500', active);
    btn.classList.toggle('text-stone-400', !active);
    btn.classList.toggle('border-transparent', !active);
}

// ── 독서 통계 ──────────────────────────────────────────────
// 집계는 서버(BookshelfService)가 담당하고 여기서는 표시만 한다.
function renderReadingStats(stats) {
    const section = document.getElementById('readingStats');
    const hasStats = Boolean(stats && stats.totalCount > 0);
    section.classList.toggle('hidden', !hasStats);

    const profileCount = document.getElementById('readBooksCount');
    if (profileCount) profileCount.textContent = hasStats ? stats.totalCount : 0;
    if (!hasStats) return;

    document.getElementById('statThisMonth').textContent = `${stats.thisMonthCount}권`;
    document.getElementById('statThisYear').textContent = `${stats.thisYearCount}권`;
    document.getElementById('statTotal').textContent = `${stats.totalCount}권`;
    renderMonthlyChart(stats.monthly);
    renderYearlyList(stats.yearly);
}

function renderMonthlyChart(monthly) {
    const chart = document.getElementById('statMonthlyChart');
    const maxCount = Math.max(...monthly.map(m => m.count), 1);
    chart.innerHTML = '';
    chart.setAttribute('aria-label', monthly.map(m => `${m.year}년 ${m.month}월 ${m.count}권`).join(', '));

    monthly.forEach((m, idx) => {
        const col = document.createElement('div');
        col.className = 'bs-chart-col';
        if (idx === monthly.length - 1) col.classList.add('current');
        col.title = `${m.year}년 ${m.month}월 ${m.count}권`;

        const track = document.createElement('div');
        track.className = 'bs-chart-track';
        if (m.count > 0) {
            const count = document.createElement('span');
            count.className = 'bs-chart-count';
            count.textContent = m.count;
            track.appendChild(count);
        }
        const bar = document.createElement('div');
        bar.className = 'bs-chart-bar';
        bar.style.setProperty('--ratio', m.count / maxCount);
        track.appendChild(bar);

        // 좁은 화면에서는 CSS로 '월'을 숨겨 숫자만 표시한다.
        const label = document.createElement('span');
        label.className = 'bs-chart-label';
        label.textContent = m.month;
        const unit = document.createElement('span');
        unit.className = 'bs-chart-unit';
        unit.textContent = '월';
        label.appendChild(unit);

        col.append(track, label);
        chart.appendChild(col);
    });
}

function renderYearlyList(yearly) {
    const list = document.getElementById('statYearlyList');
    list.innerHTML = '';
    yearly.forEach(y => {
        const chip = document.createElement('span');
        chip.className = 'bs-year';
        chip.textContent = `${y.year}년 ${y.count}권`;
        list.appendChild(chip);
    });
}

// 책장 변경은 이미 저장된 뒤이므로, 통계 갱신 실패를 저장 실패처럼 알리지 않는다.
async function refreshReadingStats() {
    try {
        renderReadingStats(await API.get('/api/bookshelf/stats'));
    } catch (e) {
        console.error('독서 통계 갱신 실패:', e);
    }
}

// ── 렌더링 ────────────────────────────────────────────────
function renderShelf() {
    buildReadOrderMaps();
    const total = bsReading.length + bsFinished.length;
    const empty = document.getElementById('shelfEmpty');
    const viewCalendar = document.getElementById('viewCalendar');
    const viewShelf = document.getElementById('viewShelf');
    const seg = document.querySelector('#panelShelf .bs-seg');

    if (total === 0) {
        empty.classList.remove('hidden');
        viewCalendar.classList.add('hidden');
        viewShelf.classList.add('hidden');
        if (seg) seg.classList.add('hidden');
        return;
    }
    empty.classList.add('hidden');
    if (seg) seg.classList.remove('hidden');

    renderReadingStrip();
    renderCalendar();

    let saved = null;
    try { saved = localStorage.getItem(BS_VIEW_KEY); } catch (e) { /* localStorage 불가 시 기본값 */ }
    setShelfView(saved === 'shelf' ? 'shelf' : 'calendar');
}

// 달력/책장 뷰 전환 (마지막 선택은 localStorage에 저장)
function setShelfView(view) {
    bsCurrentView = view === 'shelf' ? 'shelf' : 'calendar';
    const isShelf = bsCurrentView === 'shelf';
    document.getElementById('viewCalendar').classList.toggle('hidden', isShelf);
    document.getElementById('viewShelf').classList.toggle('hidden', !isShelf);
    document.querySelectorAll('#panelShelf .bs-seg-btn').forEach(b => {
        b.classList.toggle('on', b.dataset.view === bsCurrentView);
    });
    try { localStorage.setItem(BS_VIEW_KEY, bsCurrentView); } catch (e) { /* 무시 */ }
    if (isShelf) renderShelfView();
}

// ── 책 시각 속성 (책 id 시드로 고정: 새로고침해도 안 변함) ──
const BS_FILLS = ['f-terra', 'f-ochre', 'f-sage', 'f-teal', 'f-cream', 'f-clay', 'f-plum', 'f-denim', 'f-olive', 'f-rust'];
const BS_MOSAIC_MAX = 4; // 달력 한 칸에 보여주는 표지 수
const BS_BOOK_SLOT_PX = 80; // 책장에서 책 한 권이 차지하는 폭 (책 74 + 좌우 여백 3*2)

function bsHash(str) {
    let h = 2166136261;
    for (let i = 0; i < str.length; i++) {
        h ^= str.charCodeAt(i);
        h = Math.imul(h, 16777619);
    }
    return h >>> 0;
}

function bookFill(entry) {
    const key = String(entry.id || entry.bookIsbn || entry.bookTitle || '');
    return BS_FILLS[bsHash(key) % BS_FILLS.length];
}

function hasCover(entry) {
    return Boolean(entry.bookThumbnail);
}

// 고화질 → 원본 썸네일 순으로 시도하고, 둘 다 실패하면 onFail을 호출한다.
function loadCover(img, url, onFail) {
    const hiRes = hiResCover(url);
    let triedOriginal = false;
    img.onerror = () => {
        if (!triedOriginal && hiRes !== url) {
            triedOriginal = true;
            img.src = url;
            return;
        }
        img.onerror = null;
        onFail();
    };
    img.src = hiRes;
}

// 색 일러스트 위에 표지 레이어를 덮는다. 로드에 실패하면 레이어만 걷어내 일러스트로 돌아간다.
function attachCover(el, entry, onFail) {
    if (!hasCover(entry)) return;
    const layer = document.createElement('span');
    layer.className = 'bs-cover';
    const img = document.createElement('img');
    img.alt = ''; // 책 제목은 부모의 title/본문으로 이미 제공
    img.decoding = 'async';
    layer.appendChild(img);
    // 표지가 잘리지 않도록 실제 비율을 CSS에 넘겨 책 크기를 맞춘다.
    img.onload = () => el.style.setProperty('--cover-ratio', img.naturalWidth / img.naturalHeight);
    el.classList.add('has-cover');
    el.prepend(layer);
    loadCover(img, entry.bookThumbnail, () => {
        layer.remove();
        el.classList.remove('has-cover');
        el.style.removeProperty('--cover-ratio');
        if (onFail) onFail();
    });
}

// ── 읽는 중 스트립 (미니 정면 책) ──
function renderReadingStrip() {
    const strip = document.getElementById('readingStrip');
    const list = document.getElementById('readingStripList');
    if (bsReading.length === 0) {
        strip.classList.add('hidden');
        return;
    }
    strip.classList.remove('hidden');
    list.innerHTML = '';
    bsReading.forEach(entry => {
        const el = document.createElement('button');
        el.type = 'button';
        el.className = 'bs-mini-faced ' + bookFill(entry);
        const tag = readTag(entry);
        el.title = tag ? `${entry.bookTitle} · ${tag}` : entry.bookTitle; // 터치 기기는 title 툴팁이 안 뜨므로, 재독 정보는 상세 모달에서도 확인 가능
        el.onclick = () => openEntryDetail(entry);
        el.innerHTML = `<span class="mk"></span><span class="mm">${escapeHtml(entry.bookTitle)}</span>`;
        attachCover(el, entry);
        list.appendChild(el);
    });
}

// ── 달력 ──
function renderCalendar() {
    document.getElementById('calendarLabel').textContent = `${calYear}년 ${calMonth + 1}월`;
    const grid = document.getElementById('calendarGrid');
    grid.innerHTML = '';

    const firstWeekday = new Date(calYear, calMonth, 1).getDay(); // 0=일
    const daysInMonth = new Date(calYear, calMonth + 1, 0).getDate();

    for (let i = 0; i < firstWeekday; i++) grid.appendChild(emptyCell());
    for (let day = 1; day <= daysInMonth; day++) {
        const dateKey = `${calYear}-${pad(calMonth + 1)}-${pad(day)}`;
        grid.appendChild(dayCell(day, bsFinishedByDate[dateKey] || []));
    }
}

function emptyCell() {
    const cell = document.createElement('div');
    cell.className = 'bs-cell empty';
    return cell;
}

function dayCell(day, entries) {
    const cell = document.createElement('div');
    cell.className = 'bs-cell';

    const dn = document.createElement('span');
    dn.className = 'dn';
    dn.textContent = day;
    cell.appendChild(dn);

    if (entries.length > 0) {
        cell.classList.add('has');
        cell.tabIndex = 0;
        const soloTag = entries.length === 1 ? readTag(entries[0]) : null;
        cell.title = entries.length > 1
            ? `${entries.length}권 완독`
            : (soloTag ? `${entries[0].bookTitle} · ${soloTag}` : entries[0].bookTitle);

        if (entries.slice(0, BS_MOSAIC_MAX).some(hasCover)) {
            fillCellCovers(cell, entries);
        } else {
            fillCellMono(cell, entries);
        }

        cell.onclick = entries.length > 1 ? () => openDayModal(entries) : () => openEntryDetail(entries[0]);
    }
    return cell;
}

function fillCellMono(cell, entries) {
    const mini = document.createElement('span');
    mini.className = 'mini ' + bookFill(entries[0]);
    if (entries.length > 1) {
        const plus = document.createElement('span');
        plus.className = 'plus';
        plus.textContent = '+' + (entries.length - 1);
        mini.appendChild(plus); // 책 모서리에 부착 (날짜 숫자와 충돌 방지)
    }
    cell.appendChild(mini);

    const bt = document.createElement('span');
    bt.className = 'bt';
    bt.textContent = entries[0].bookTitle;
    cell.appendChild(bt);
}

// 표지 모자이크: 1권은 칸 전체, 2권은 좌우 분할, 3권 이상은 2×2 (최대 4장) + 완독 권수 배지
function fillCellCovers(cell, entries) {
    const shown = entries.slice(0, BS_MOSAIC_MAX);
    const mosaic = document.createElement('span');
    mosaic.className = `bs-mosaic n${Math.min(shown.length, 3)}`;

    const count = document.createElement('span');
    count.className = 'bs-count';
    count.textContent = entries.length;

    // 표지가 하나도 안 뜨면 색 타일만 남으므로 칸 전체를 일러스트로 되돌린다.
    let remaining = shown.filter(hasCover).length;
    const onCoverFail = () => {
        remaining -= 1;
        if (remaining > 0) return;
        cell.classList.remove('cover-cell');
        mosaic.remove();
        count.remove();
        fillCellMono(cell, entries);
    };

    shown.forEach(entry => {
        const tile = document.createElement('span');
        tile.className = 'bs-tile ' + bookFill(entry);
        attachCover(tile, entry, onCoverFail);
        mosaic.appendChild(tile);
    });

    cell.classList.add('cover-cell');
    cell.appendChild(mosaic);
    if (entries.length > 1) cell.appendChild(count);
}

// ── 책장 뷰 (연도별 그룹, 여러 단으로 wrap) ──
function renderShelfView() {
    const host = document.getElementById('viewShelf');
    const scene = document.getElementById('bsScene');
    if (!host || host.classList.contains('hidden')) return;
    const innerW = scene.clientWidth - 40; // scene 좌우 패딩(20*2)
    if (innerW <= 0) return; // 아직 화면에 없음 (탭 전환 시 다시 렌더)
    host.innerHTML = '';

    if (bsReading.length > 0) {
        renderShelfSection(host, { label: '읽는 중', now: true, count: bsReading.length },
            bsReading, { plant: true }, innerW);
    }

    const byYear = {};
    bsFinished.forEach(e => {
        const y = (e.finishedAt || '').slice(0, 4) || '기타';
        (byYear[y] = byYear[y] || []).push(e);
    });
    Object.keys(byYear).sort((a, b) => b.localeCompare(a)).forEach(year => {
        const list = byYear[year];
        renderShelfSection(host, { label: year, count: list.length },
            list, {}, innerW);
    });
}

// 한 그룹을 라벨 + 여러 선반 줄로 렌더 (책 칸 너비 합으로 줄바꿈)
function renderShelfSection(host, meta, entries, opts, innerW) {
    const wrap = document.createElement('div');
    wrap.className = 'bs-shelf';

    const label = document.createElement('span');
    label.className = 'bs-shelf-label' + (meta.now ? ' now' : '');
    label.innerHTML = `${escapeHtml(meta.label)} <span class="n">${meta.now ? meta.count + '권' : '· ' + meta.count}</span>`;
    wrap.appendChild(label);

    const items = entries.map(entry => ({ entry, w: BS_BOOK_SLOT_PX }));
    if (opts.plant) items.push({ plant: true, w: 54 });

    const gap = 7;
    let row = [];
    let rowW = 0;
    const flush = () => {
        if (!row.length) return;
        const books = document.createElement('div');
        books.className = 'bs-books';
        row.forEach(it => {
            books.appendChild(it.plant ? makePlant() : makeFaced(it.entry));
        });
        wrap.appendChild(books);
        const plank = document.createElement('div');
        plank.className = 'bs-plank';
        wrap.appendChild(plank);
        row = [];
        rowW = 0;
    };
    items.forEach(it => {
        const w = it.w + gap;
        if (rowW + w > innerW && row.length) flush();
        row.push(it);
        rowW += w;
    });
    flush();

    host.appendChild(wrap);
}

function makeFaced(entry) {
    const el = document.createElement('button');
    el.type = 'button';
    el.className = 'bs-bkf ' + bookFill(entry);
    const tagFull = readTag(entry);
    el.title = tagFull ? `${entry.bookTitle} · ${tagFull}` : entry.bookTitle;
    const mark = entry.finishedAt ? '' : '<span class="mark"></span>';
    const band = readBand(entry);
    const badge = band ? `<span class="reread">${escapeHtml(band)}</span>` : '';
    el.innerHTML = `${mark}${badge}<span class="ct">${escapeHtml(entry.bookTitle)}</span><span class="ca">${escapeHtml(entry.bookAuthor)}</span>`;
    attachCover(el, entry);
    el.onclick = () => openEntryDetail(entry);
    return el;
}

function makePlant() {
    const el = document.createElement('div');
    el.className = 'bs-plant';
    el.setAttribute('aria-hidden', 'true');
    el.innerHTML = '<span class="leaf l3"></span><span class="leaf l1"></span><span class="leaf l2"></span><span class="pot"></span>';
    return el;
}

// 같은 날 완독한 여러 책 목록 모달
function openDayModal(entries) {
    const list = document.getElementById('dayBooksList');
    const finishedAt = entries[0] && entries[0].finishedAt;
    document.getElementById('dayModalTitle').textContent =
        `${finishedAt ? shortDate(finishedAt) + ' ' : ''}완독한 책 ${entries.length}권`;
    list.innerHTML = '';
    entries.forEach(entry => {
        const row = document.createElement('button');
        row.type = 'button';
        row.className = 'flex gap-3 items-center p-2 rounded-lg hover:bg-stone-50 text-left w-full';
        row.onclick = () => { closeDayModal(); openEntryDetail(entry); };
        const tag = readTag(entry);
        row.innerHTML = `
            <div class="w-10 h-14 flex-shrink-0 rounded overflow-hidden bg-stone-100">
                ${thumbHtml(entry, 'w-full h-full object-cover')}
            </div>
            <div class="min-w-0">
                <div class="font-semibold text-sm text-stone-800 line-clamp-1">${escapeHtml(entry.bookTitle)}</div>
                <div class="text-xs text-stone-500 line-clamp-1">${escapeHtml(entry.bookAuthor)}</div>
                ${tag ? `<div class="text-xs text-amber-600 font-bold mt-0.5">🔁 ${escapeHtml(tag)}</div>` : ''}
            </div>`;
        list.appendChild(row);
    });
    Modal.open('dayBooksModal');
}

function closeDayModal() {
    Modal.close('dayBooksModal');
}

function bookshelfChangeMonth(delta) {
    calMonth += delta;
    if (calMonth < 0) { calMonth = 11; calYear--; }
    else if (calMonth > 11) { calMonth = 0; calYear++; }
    renderCalendar();
}

// ── 항목 상세/편집 ─────────────────────────────────────────
function openEntryDetail(entry) {
    bsEditingEntry = entry;
    document.getElementById('edTitle').textContent = entry.bookTitle;
    document.getElementById('edAuthor').textContent = entry.bookAuthor;
    setThumbnail('edThumbnail', entry);

    const tagEl = document.getElementById('edReadTag');
    const tag = readTag(entry);
    tagEl.textContent = tag ? `🔁 ${tag}` : '';
    tagEl.classList.toggle('hidden', !tag);

    const ownerControls = document.getElementById('edOwnerControls');
    const readonly = document.getElementById('edReadonly');

    if (bsIsOwner) {
        readonly.classList.add('hidden');
        ownerControls.classList.remove('hidden');
        document.getElementById('edStartedAt').value = entry.startedAt || '';
        document.getElementById('edFinishedAt').value = entry.finishedAt || '';
    } else {
        ownerControls.classList.add('hidden');
        readonly.classList.remove('hidden');
        const started = entry.startedAt ? shortDate(entry.startedAt) : '―';
        const finished = entry.finishedAt ? shortDate(entry.finishedAt) : '읽는 중';
        readonly.innerHTML = `시작 ${started} · 완독 ${finished}`;
    }
    Modal.open('entryDetailModal');
}

function closeEntryDetailModal() {
    Modal.close('entryDetailModal');
    bsEditingEntry = null;
}

async function bsSaveEntryEdit() {
    if (!bsEditingEntry) return;
    const startedAt = document.getElementById('edStartedAt').value || null;
    const finishedAt = document.getElementById('edFinishedAt').value || null;
    if (startedAt && finishedAt && finishedAt < startedAt) {
        Alert.error('완독일은 시작일보다 앞설 수 없습니다.');
        return;
    }
    const editDupMsg = duplicateReason(bsEditingEntry.bookIsbn, finishedAt, bsEditingEntry.id);
    if (editDupMsg) {
        Alert.error(editDupMsg);
        return;
    }
    try {
        const updated = await API.put(`/api/bookshelf/${bsEditingEntry.id}`, { startedAt, finishedAt });
        const entry = toClientEntry(updated);
        bsReading = bsReading.filter(e => e.id !== entry.id);
        bsFinished = bsFinished.filter(e => e.id !== entry.id);
        if (entry.finishedAt) bsFinished.push(entry); else bsReading.push(entry);
        bsSortEntries();
        closeEntryDetailModal();
        bsRerender();
        refreshReadingStats();
    } catch (e) {
        Alert.error(e.message);
    }
}

async function bsDeleteEntry() {
    if (!bsEditingEntry) return;
    if (!Alert.confirm('이 책을 책장에서 삭제할까요?')) return;
    try {
        const id = bsEditingEntry.id;
        await API.delete(`/api/bookshelf/${id}`);
        bsReading = bsReading.filter(e => e.id !== id);
        bsFinished = bsFinished.filter(e => e.id !== id);
        closeEntryDetailModal();
        bsRerender();
        refreshReadingStats();
    } catch (e) {
        Alert.error(e.message);
    }
}

// ── 책 추가 모달 ───────────────────────────────────────────
function openAddBookModal() {
    bsClearSelection();
    document.getElementById('bsBookList').innerHTML = '';
    document.getElementById('bsBookSearch').value = '';
    Modal.open('addBookModal');
    setTimeout(() => document.getElementById('bsBookSearch').focus(), 50);
}

function closeAddBookModal() {
    Modal.close('addBookModal');
}

async function bsSearchBooks() {
    const query = document.getElementById('bsBookSearch').value.trim();
    if (!query) {
        Alert.error('검색어를 입력하세요');
        return;
    }
    const list = document.getElementById('bsBookList');
    const loading = document.getElementById('bsBookLoading');
    list.innerHTML = '';
    loading.classList.remove('hidden');

    try {
        const data = await API.get(`/api/external/books/search?query=${encodeURIComponent(query)}&page=1&size=12`);
        loading.classList.add('hidden');
        const docs = (data && data.documents) || [];
        if (docs.length === 0) {
            list.innerHTML = '<p class="text-stone-500 col-span-full text-center py-4">검색 결과가 없습니다.</p>';
            return;
        }
        docs.forEach(book => {
            const item = document.createElement('div');
            item.className = 'flex flex-col p-3 border-2 border-stone-200 rounded-lg cursor-pointer transition-all hover:border-amber-500 hover:bg-amber-50';
            item.onclick = () => bsSelectBook(book);
            item.innerHTML = `
                ${coverImgTag(book.thumbnail, 'w-full h-40 object-contain rounded mb-2', book.title)}
                <div class="font-semibold text-sm mb-0.5 line-clamp-2">${escapeHtml(book.title)}</div>
                <div class="text-stone-500 text-xs line-clamp-1">${escapeHtml((book.authors || []).join(', '))}</div>
            `;
            list.appendChild(item);
        });
    } catch (e) {
        loading.classList.add('hidden');
        list.innerHTML = '<p class="text-red-500 col-span-full text-center py-4">검색에 실패했습니다.</p>';
    }
}

function bsSelectBook(book) {
    bsSelectedBook = {
        bookTitle: book.title,
        bookAuthor: (book.authors || []).join(', '),
        bookIsbn: book.isbn || '',
        bookThumbnail: book.thumbnail || null,
    };
    document.getElementById('bsSelectedTitle').textContent = bsSelectedBook.bookTitle;
    document.getElementById('bsSelectedAuthor').textContent = bsSelectedBook.bookAuthor;
    document.getElementById('bsSelectedThumbnail').src = bsSelectedBook.bookThumbnail || '';
    document.getElementById('bsSearchSection').classList.add('hidden');
    document.getElementById('bsSelectedSection').classList.remove('hidden');
}

function bsClearSelection() {
    bsSelectedBook = null;
    document.getElementById('bsStartedAt').value = '';
    document.getElementById('bsFinishedAt').value = '';
    document.getElementById('bsSelectedSection').classList.add('hidden');
    document.getElementById('bsSearchSection').classList.remove('hidden');
}

async function bsSubmitEntry() {
    if (!bsSelectedBook) return;
    const startedAt = document.getElementById('bsStartedAt').value || null;
    const finishedAt = document.getElementById('bsFinishedAt').value || null;
    if (startedAt && finishedAt && finishedAt < startedAt) {
        Alert.error('완독일은 시작일보다 앞설 수 없습니다.');
        return;
    }
    const addDupMsg = duplicateReason(bsSelectedBook.bookIsbn, finishedAt, null);
    if (addDupMsg) {
        Alert.error(addDupMsg);
        return;
    }
    try {
        const created = await API.post('/api/bookshelf', { ...bsSelectedBook, startedAt, finishedAt });
        const entry = toClientEntry(created);
        if (entry.finishedAt) {
            bsFinished.push(entry);
            const d = new Date(entry.finishedAt); // 완독 등록 시 해당 달로 달력 이동
            calYear = d.getFullYear();
            calMonth = d.getMonth();
        } else {
            bsReading.push(entry);
        }
        bsSortEntries();
        closeAddBookModal();
        bsRerender();
        refreshReadingStats();
    } catch (e) {
        Alert.error(e.message);
    }
}

// ── 헬퍼 ─────────────────────────────────────────────────
function setThumbnail(imgId, entry) {
    const img = document.getElementById(imgId);
    if (entry.bookThumbnail) {
        img.src = hiResCover(entry.bookThumbnail);
        img.onerror = function () { img.onerror = null; img.src = entry.bookThumbnail; }; // 폴백
        img.classList.remove('hidden');
    } else {
        img.removeAttribute('src');
        img.classList.add('hidden');
    }
}

function thumbHtml(entry, imgClass) {
    if (entry.bookThumbnail) {
        return coverImgTag(entry.bookThumbnail, imgClass, entry.bookTitle);
    }
    return `<div class="w-full h-full flex items-center justify-center bg-stone-200 text-stone-400 text-lg">📖</div>`;
}

// 중복 등록 사유 반환 (없으면 null). ISBN 없으면 검사 안 함
// - 읽는 중: 같은 책이 이미 읽는 중이면 차단
// - 완독: 같은 책을 같은 완독일로 등록했으면 차단(다른 날짜는 재독 허용)
function duplicateReason(bookIsbn, finishedAt, excludeId) {
    if (!bookIsbn) return null;
    if (!finishedAt) {
        const dup = bsReading.some(e => e.id !== excludeId && e.bookIsbn === bookIsbn);
        return dup ? "이미 '읽는 중'으로 등록된 책입니다." : null;
    }
    const dup = bsFinished.some(e =>
        e.id !== excludeId && e.bookIsbn === bookIsbn && e.finishedAt === finishedAt);
    return dup ? '이미 같은 날짜에 완독한 책으로 등록되어 있습니다.' : null;
}

function pad(n) {
    return String(n).padStart(2, '0');
}

function shortDate(isoDate) {
    // 'YYYY-MM-DD' -> 'YYYY.MM.DD'
    return isoDate.replace(/-/g, '.');
}

function escapeHtml(str) {
    return String(str == null ? '' : str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
