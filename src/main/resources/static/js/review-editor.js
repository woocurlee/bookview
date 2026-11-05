// TipTap 에디터 초기화. CSP상 인라인 import map을 쓸 수 없어 CDN URL을 직접 import한다.
import { Editor } from 'https://esm.sh/@tiptap/core@2.11.5'
import StarterKit from 'https://esm.sh/@tiptap/starter-kit@2.11.5'
import Link from 'https://esm.sh/@tiptap/extension-link@2.11.5'
import Placeholder from 'https://esm.sh/@tiptap/extension-placeholder@2.11.5'

const editor = new Editor({
    element: document.querySelector('#editor'),
    extensions: [
        StarterKit.configure({
            heading: { levels: [1, 2, 3] },
            horizontalRule: false
        }),
        Link.configure({ openOnClick: false, autolink: true }),
        Placeholder.configure({ placeholder: '책을 읽으며 느낀 점을 자유롭게 작성해보세요...' })
    ],
    content: ''
})

const toolbar = document.querySelector('#editorToolbar')

// 핵심: pointerdown에서 preventDefault → 툴바 탭이 에디터 선택을 지우지 않게 (모바일 서식 토글)
toolbar.addEventListener('pointerdown', (e) => {
    const btn = e.target.closest('button[data-cmd]')
    if (!btn) return
    e.preventDefault()
    runCmd(btn.dataset.cmd)
})

function runCmd(c) {
    const chain = editor.chain().focus()
    switch (c) {
        case 'h1': chain.toggleHeading({ level: 1 }).run(); break
        case 'h2': chain.toggleHeading({ level: 2 }).run(); break
        case 'h3': chain.toggleHeading({ level: 3 }).run(); break
        case 'paragraph': chain.setParagraph().run(); break
        case 'bold': chain.toggleBold().run(); break
        case 'italic': chain.toggleItalic().run(); break
        case 'strike': chain.toggleStrike().run(); break
        case 'blockquote': chain.toggleBlockquote().run(); break
        case 'code': chain.toggleCode().run(); break
        case 'codeBlock': chain.toggleCodeBlock().run(); break
        case 'link': setLink(); break
    }
}

function setLink() {
    const prev = editor.getAttributes('link').href || ''
    const url = window.prompt('링크 URL', prev)
    if (url === null) return
    if (url === '') {
        editor.chain().focus().extendMarkRange('link').unsetLink().run()
        return
    }
    editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

// 툴바 버튼 활성 상태 동기화
function setActive(cmd, on) {
    const btn = toolbar.querySelector(`button[data-cmd="${cmd}"]`)
    if (btn) btn.classList.toggle('active', on)
}

function refresh() {
    ['bold', 'italic', 'strike', 'code', 'codeBlock', 'blockquote'].forEach(s => setActive(s, editor.isActive(s)))
    setActive('h1', editor.isActive('heading', { level: 1 }))
    setActive('h2', editor.isActive('heading', { level: 2 }))
    setActive('h3', editor.isActive('heading', { level: 3 }))
    setActive('link', editor.isActive('link'))
}

editor.on('selectionUpdate', refresh)
editor.on('transaction', refresh)
refresh()

// write-review.js(클래식 스크립트)에서 사용할 API
window.reviewEditor = {
    getHTML: () => editor.getHTML(),
    getText: () => editor.getText(),
    setContent: (html) => editor.commands.setContent(html || '', false)
}
