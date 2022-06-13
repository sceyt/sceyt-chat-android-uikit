/*
 * Copyright (C) 2020 - Amir Hossein Aghajari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.emojiview.emojiview.listener;

public abstract class SimplePopupAdapter implements PopupListener {
    @Override
    public void onDismiss() {
    }

    @Override
    public void onShow() {
    }

    @Override
    public void onKeyboardOpened(int height) {
    }

    @Override
    public void onKeyboardClosed() {
    }

    @Override
    public void onViewHeightChanged(int height) {
    }
}
