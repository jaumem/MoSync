/*
Copyright (C) 2011 MoSync AB

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License,
version 2, as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA 02110-1301, USA.
*/

/**
 * @file StackScreen.h
 * @author Emma Tresanszki
 */

#ifndef NATIVEUI_STACK_SCREEN_H_
#define NATIVEUI_STACK_SCREEN_H_

#include <ma.h>

#include <MAUtil/Vector.h>

#include <IX_WIDGET.h>

#include "Screen.h"

namespace NativeUI
{
    // Forward declaration.
    class StackScreenListener;

    class StackScreen : public Screen
    {
    public:
        /**
         * Constructor.
         */
        StackScreen();

        /**
         * Destructor.
         */
        virtual ~StackScreen();

        /*
         * Get the total number of the screens in the stack.
         */
        virtual int getStackSize();

        /*
         * Push a screen inside the stack.
         * @param screen The new screen.
         */
        virtual void push(Screen* screen);

        /**
         * Pops a screen from a screen stack, hides the current screen and shows
         * the previous screen.
         * If there is no previous screen in the screen stack,
         * an empty screen will be shown.
         */
        virtual void pop();

        /**
         * Specify whether the back button automatically
         * should pop the stack screen.
         * This can have different behaviors on different platforms:
         * on iPhone the UI back button is disabled,
         * on Android the stack screen ignores back button events.
         * @param enabledState True if the back button is enabled, false otherwise.
         */
        virtual void enableBackButton(const bool enabledState);

        /**
         * Add an stack screen event listener.
         * @param listener The listener that will receive stack screen events.
         */
        virtual void addStackScreenListener(StackScreenListener* listener);

        /**
         * Remove the stack screen listener.
         * @param listener The listener that receives stack screen events.
         */
        virtual void removeStackScreenListener(StackScreenListener* listener);

    protected:
        /**
         * This method is called when there is an event for this widget.
         * It passes on the event to all widget's listeners.
         * @param widgetEventData The data for the widget event.
         */
        virtual void handleWidgetEvent(MAWidgetEventData* widgetEventData);

    private:
        /**
         * Array with stack screen listeners.
         */
        MAUtil::Vector<StackScreenListener*> mStackScreenListeners;

        /**
         * A Stack of Screens, the top of the stack must always be the screen
         * that is being shown.
         */
        MAUtil::Vector<Screen*> mStack;
    };

} // namespace NativeUI

#endif /* NATIVEUI_STACK_SCREEN_H_ */
