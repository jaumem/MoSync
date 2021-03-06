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
 * @file TabScreen.h
 * @author Niklas Nummelin & Mikael Kindborg
 *
 * Class that represents a screen with tabs.
 */

#include "TabScreen.h"

namespace MoSync
{
	namespace UI
	{

	/**
	 * Constructor.
	 */
	TabScreen::TabScreen() :
		Screen(MAW_TAB_SCREEN)
	{
	}

	/**
	 * Destructor.
	 */
	TabScreen::~TabScreen()
	{
	}

	/**
	 * Add a new tab with a screen in it.
	 * @param screen The screen shown in the new tab.
	 */
	void TabScreen::addTab(Screen* screen)
	{
		addChild(screen);
	}

	/**
	 * Show a given tab.
	 * @param index The index of the tab to show.
	 * Index starts at zero.
	 */
	void TabScreen::setActiveTab(int index)
	{
		setProperty("selectedTab", index);
	}

	/**
	 * Returns the index of the current tab.
	 * Index starts at zero.
	 */
	int TabScreen::getActiveTab()
	{
		return getPropertyInt(MAW_TAB_SCREEN_CURRENT_TAB);
	}

	} // namespace UI
} // namespace MoSync
