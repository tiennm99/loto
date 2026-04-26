"use client";

import Link from "next/link";
import { useState } from "react";
import PlayerBoard from "@/components/player-board";

export default function Home() {
  const [showInstructions, setShowInstructions] = useState(false);

  return (
    <div className="flex flex-col flex-1 items-center px-3 py-8 sm:py-12">
      <div className="w-full max-w-lg">
        {/* Header */}
        <header className="text-center mb-8">
          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-500 to-purple-500 bg-clip-text text-transparent">
            Lô tô
          </h1>
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
            Lấy cảm hứng từ những buổi họp lớp thiếu giấy chơi lô tô
            <br className="hidden sm:block" /> của TN1 (2014–2017)
          </p>
          <div className="mt-3 flex items-center justify-center gap-3 text-xs">
            <button
              onClick={() => setShowInstructions((v) => !v)}
              className="text-indigo-500 dark:text-indigo-400 hover:underline"
            >
              {showInstructions ? "Ẩn hướng dẫn" : "Hướng dẫn"}
            </button>
            <span className="text-slate-300 dark:text-slate-600">|</span>
            <Link
              href="/master"
              className="text-orange-500 dark:text-orange-400 hover:underline"
            >
              Trang quản trò →
            </Link>
          </div>
        </header>

        {/* Instructions */}
        {showInstructions && (
          <div className="mb-6 rounded-xl bg-indigo-50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900 p-4 text-sm text-slate-600 dark:text-slate-400">
            <ul className="space-y-1 list-disc list-inside">
              <li>
                Nhấn{" "}
                <strong className="text-slate-800 dark:text-slate-200">
                  Tạo bảng mới
                </strong>{" "}
                để tạo bảng
              </li>
              <li>Nhấn vào ô số để đánh dấu khi số được xổ</li>
              <li>Nhấn lại để bỏ đánh dấu</li>
              <li>Bảng được lưu tự động</li>
            </ul>
          </div>
        )}

        <PlayerBoard />

        {/* Footer */}
        <footer className="mt-10 text-center text-xs text-slate-400 dark:text-slate-600">
          Made with ❤️ by{" "}
          <a
            href="https://miti99.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-500 hover:underline"
          >
            miti99
          </a>
        </footer>
      </div>
    </div>
  );
}
