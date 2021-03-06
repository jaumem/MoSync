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
 * @file TextWidget.cpp
 * @author Mikael Kindborg
 *
 * Base class for widgets that have a text property.
 */

#include <mavsprintf.h>
#include <mastdlib.h>
#include "TextWidget.h"

namespace MoSync
{
	namespace UI
	{

	/**
	 * Constructor is protected because actual widget instances
	 * should be subclasses of this class.
	 * @widgetType The string constant that identifies the widget type
	 * (one of the MAW_ constants).
	 */
	TextWidget::TextWidget(const MAUtil::String& widgetType) :
		Widget(widgetType)
	{
	}

	/**
	 * Destructor.
	 */
	TextWidget::~TextWidget()
	{
	}

	/**
	 * Set the text of the widget.
	 * Note: Not all widgets support this property.
	 * @param text The text to be displayed by the widget.
	 */
	void TextWidget::setText(const MAUtil::String& text)
	{
		setProperty("text", text.c_str());
	}

	/**
	 * Center the text of the widget horizontally.
	 * Note: Not all widgets support this property.
	 */
	void TextWidget::centerTextHorizontally()
	{
		setProperty("textHorizontalAlignment", MAW_ALIGNMENT_CENTER);
	}

	/**
	 * Center the text of the widget vertically.
	 * Note: Not all widgets support this property.
	 */
	void TextWidget::centerTextVertically()
	{
		setProperty("textVerticalAlignment", MAW_ALIGNMENT_CENTER);
	}

	/**
	 * Sets the font color of the widget.
	 * @param color The font color of the widget.
	 */
	void TextWidget::setFontColor(const int color)
	{
		char buffer[256];
		sprintf(buffer, "0x%.6X", color);
		setProperty(MAW_LABEL_FONT_COLOR, buffer);
	}

	/**
	 * Set the font color of the widget.
	 * Note: Not all widgets support this property.
	 * @param red Red component (range 0-255).
	 * @param green Green component (range 0-255).
	 * @param blue Blue component (range 0-255).
	 */
	void TextWidget::setFontColor(int red, int green, int blue)
	{
		char buffer[256];
		sprintf(buffer, "0x%X%X%X", red, green, blue);
		setProperty("fontColor", buffer);
	}

	/**
	 * Set the font size of the widget.
	 * Note: Not all widgets support this property.
	 * @param size The font size.
	 */
	void TextWidget::setFontSize(int size)
	{
		setProperty("fontSize", size);
	}

	} // namespace UI
} // namespace MoSync
