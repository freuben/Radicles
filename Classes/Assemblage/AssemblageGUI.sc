// Assemblage: windows: the mixer, effect windows, master output and direct-input recording windows.
// Part of the Assemblage class: the class and its variables are in Assemblage.sc.

+ Assemblage {
	mixWinBool {arg action={};
		if(mixerWin.notNil, {
			if(mixerWin.notClosed, {
				action.();
			});
		});
	}

	mixGUI {
		var sends, knobColors, winHeight, winWidth, knobSize, knobTextSize, canvas,
		panKnobTextArr, panKnobArr, sliderTextArr, sliderArr, levelTextArr,
		levelArr, vlay, sendsMenuArr, sendsKnobArr, outputMenuArr,
		muteButton, recButton, soloButton, spaceButton, volButtons, buttonsLay1,
		buttonsLay2, oscDefFunc, levelSoloStates, fxSlotArr, trackLabelArr, spaceTextLay, panKnobLay,
		popupmenusize, mixInputLabels, trackInputSel, inputArray,
		numBuses, thisInputLabel, busInLabels, maxBusIn, knobFunc, busInSettings,
		guiFunc, fltMenuWindow, oldMixerWin, slotsSizeArr, sumWidth, spaceGap,
		sumHeight, inKnob, gapHeight, peakMax, inKnobArr, inputMenuArr;

		this.updateMixInfo; //update info

		//getting input label data
		inputArray = 	inputs.flop[0].collect{|item| item.asString.replace("space").toLower.asSymbol; };
		mixTrackNames.do{|item|
			if(inputArray.includes(item), {
				thisInputLabel = inputs.flop[1][inputArray.indexOf(item)];
				if(thisInputLabel.isArray, {thisInputLabel = thisInputLabel.collect{|item| item.key};
				}, {
					thisInputLabel = thisInputLabel.key;});
				if(item != \master, {
					trackInputSel = trackInputSel.add([mixTrackNames.indexOf(item),
						thisInputLabel ]); });
			});
		};

		busInLabels = this.getBusInLabels;
		if(busInLabels.isNil, {
			sends = 2;
		}, {
			maxBusIn = busInLabels.flop[1].collect{|item| item.size}.maxItem;
			if(maxBusIn >= 2, {
				sends = maxBusIn + 1;
			}, {
				sends = 2;
			});
		});

		fxsNum = 2;
		if(filters.notNil, {
			if(filters.notEmpty, {
				fxsNum = filters.flop[0].collect({|item|
					item.asString.split($_).last.asInteger }).maxItem.max(1) + 1;
			});
		});

		levelSoloStates = sysChans.collect({|item, index| 1!item }).flat;

		muteButArr = [];
		soloButArr = [];
		recButArr = [];
		spaceButArr = [];

		numBuses = trackNames.select{|item| item.asString.find("bus").notNil }.size+1;

		knobColors = [ Color(0.91764705882353, 0.91764705882353, 0.91764705882353),
			Color.white, Color.black, Color() ];

		winHeight = 504 + ((sends-2)*15) + ((fxsNum-2)*15);
		winWidth = (42*(sysChans.sum));
		/*if(sysPan.includes(1), {knobSize = 40;}, {knobSize = 30; });*/

		if(winRefresh, {oldMixerWin=mixerWin; winRefresh = false;
			{
				nodeTime.yield;
				oldMixerWin.close;
				server.latency.yield;
				server.sync;
				oscDefFunc.();
			}.fork(AppClock);
		});
		mixerWin = ScrollView().name_("Assemblage");
		mixerWin.mouseDownAction = {
			if(fltMenuWindow.notNil, {
				if(fltMenuWindow.visible, {
					fltMenuWindow.close;
					fltMenuWindow = nil;
				});
			});
			if(mastOutWin.notNil, {
				if(mastOutWin.visible, {
					mastOutWin.close;
					mastOutWin = nil;
				});
			});
			if(winDirRec.notNil, {
				if(winDirRec.visible, {
					winDirRec.close;
					winDirRec = nil;
				});
			});
		};
		mixerWin.onMove = {
			if(fltMenuWindow.notNil, {
				if(fltMenuWindow.visible, {
					fltMenuWindow.close;
					fltMenuWindow = nil;
				});
			});
		};

		canvas = View();
		canvas.background_(Color.black);
		srcButtonArr = [];

		sysChans.do{|item, index|
			var slider, level, sliderText, levelText, hlay, thisLay, ts, finalLayout, slotsSize,
			panKnob, panKnob1, panKnob2, panKnob3, panKnobText, panKnobText1,
			panKnobText2, panKnobText3, outputMenu, outputLabel,
			sendsMenu, sendsLabel, sendsKnobs, sendsLay, inputMenu, inputLabel, srcButton,
			inputLabelArr, mixInputLabelArr, fxLabel, fxSlot, trackLabel, trackColor,
			thisInputVal, butUIHeight, butUIWidth, sendsString, soloButtonFunc, panKeyValues, panKey;
			//volume slider
			sliderText = StaticText(canvas).align_(\center)
			.background_(Color.black).stringColor_(Color.white)
			.minWidth_(24).maxWidth_(24).maxHeight_(10).minHeight_(10);
			sliderTextArr = sliderTextArr.add(sliderText);
			slider = Slider().minWidth_(20).maxWidth_(20).maxHeight_(180).minHeight_(180)
			.focusColor_(Color.red(alpha:0.2))
			.background_(Color.black);

			//levelIndicator
			levelText = StaticText(canvas).align_(\center)
			/*.background_(colorMeter).stringColor_(Color.white)*/
			.background_(Color.black).stringColor_(Color.white)
			.minWidth_(24).maxHeight_(10).minHeight_(10);
			levelTextArr = levelTextArr.add(levelText);

			item.do({
				level = level.add(LevelIndicator()
					.drawsPeak_(true)
					/*	.style_(\led)*/
					.warning_(0.9)
					.critical_(0.9999)
					.minWidth_(10)
					.maxWidth_(10)
				);
			});
			hlay = HLayout(sliderText, levelText);

			//panning knob(s)/slider2D(s)
			panKey = ("space" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			panKeyValues = Ndef(panKey).getKeysValues.reject({|item| item.asString.contains("wet") });
			knobSize = 30 * (1.2 - (0.2* panKeyValues.size ) );
			knobTextSize = 24 * (1.2 - (0.2* panKeyValues.size ) );
			if(panKeyValues.flop[0].includes(\dirmul).not, {
				/*if(sysPan[index] == 0, {
					panKnob = Knob().minWidth_(knobSize).maxWidth_(knobSize)
					.maxHeight_(knobSize).minHeight_(knobSize).centered_(true);
					panKnob.color = knobColors;
					panKnobText = StaticText(canvas).align_(\center).background_(Color.black)
					.stringColor_(Color.white).minWidth_(24).maxWidth_(24).maxHeight_(10).minHeight_(10);
					panKnobText = [panKnobText];
				}, {*/
					panKeyValues.do{
						var knob, staticText;
						knob = Knob().minWidth_(knobSize).maxWidth_(knobSize)
							.maxHeight_(knobSize).minHeight_(knobSize).centered_(true);
						knob.color = knobColors;
						panKnob = panKnob.add(knob);
						staticText = 	StaticText(canvas).align_(\center).background_(Color.black)
					.stringColor_(Color.white).minWidth_(knobTextSize).maxWidth_(knobTextSize)
					.maxHeight_(10).minHeight_(10);
						staticText.string = "0";
						panKnobText = panKnobText.add(staticText);
					};
				/*});*/
			}, {
				panKnob = nil;
			});

			panKnobTextArr = panKnobTextArr.add(panKnobText);
			spaceTextLay = HLayout(*panKnobText);
			panKnobArr = panKnobArr.add(panKnob);
			if(panKnob.notNil, {
				panKnobLay = HLayout(*panKnob);
			}, {
				panKnobLay = 	panKnob;
			});

			butUIHeight = 12;
			butUIWidth = 25;

			if(index == (mixTrackNames.size-1), {
				soloButton = StaticText(canvas).minWidth_(butUIWidth).maxWidth_(butUIWidth)
				.maxHeight_(butUIHeight).minHeight_(butUIHeight)
				.background_(Color.black);
				soloButton.string= "     ";
			}, {

				soloButtonFunc = {var mastChans;
					if(soloStates.includes(1), {
						levelSoloStates = sysChans.collect({|item, index|
							if(soloStates[index].isNil, {
								1!item;
							}, {
								soloStates[index]!item;
							});
						}).flat;
					}, {
						levelSoloStates = sysChans.collect({|item, index| 1!item }).flat;
					});

					mastChans = Ndef(\master).numChannels;
					levelSoloStates = levelSoloStates.copyRange(0,
						(levelSoloStates.size-1) - mastChans)
					++ (1!mastChans);
				};

				soloButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
				.maxHeight_(butUIHeight).minHeight_(butUIHeight)
				.focusColor_(Color.red(alpha:0.2))
				.background_(Color.black);
				soloButton.states = [["S", Color.white, Color.black],
					["S", Color.white, colorWarning]];
				soloButton.font = basicFont;
				soloButton.srcAction = {|butt|
					soloStates[index] = butt.value;
					soloButtonFunc.();
					this.masterSoloFunc2;
				};
				soloButton.value = soloStates[index];
				soloButtonFunc.();
			});

			recButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
			.maxHeight_(butUIHeight).minHeight_(butUIHeight)
			.focusColor_(Color.red(alpha:0.2))
			.background_(Color.black);
			recButton.states = [["R", Color.white, Color.black],
				["R", Color.white, colorCritical]];
			recButton.font = basicFont;
			recButton.srcAction = {|butt|
				var thisVal;
				thisVal = butt.value;
				recStates[index] = thisVal;
			};
			recButton.value = recStates[index];

			muteButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
			.maxHeight_(butUIHeight).minHeight_(butUIHeight)
			.background_(Color.black);
			muteButton.states = [["M", Color.white, Color.black],
				["M", Color.white, colorTrack]];
			muteButton.font = basicFont;
			muteButton.srcAction = {|butt|
				var thisVal;
				thisVal = butt.value;
				("Ndef(" ++ mixTrackNames[index].cs ++ ").set('mute', " ++ thisVal
					++ ");").radcode;
				muteStates[index] = thisVal;
			};
			muteButton.value = muteStates[index];

			spaceButton = Button().minWidth_(butUIWidth).maxWidth_(butUIWidth)
			.maxHeight_(butUIHeight).minHeight_(butUIHeight)
			.focusColor_(Color.red(alpha:0.2))
			.background_(Color.black);
			spaceButton.states = [["◯", Color.white, Color.black],
				["◯", Color.white, Color.black]];
			spaceButton.font = basicFont;
			spaceButton.srcAction = {|butt|
				"space is the place".postln;
			};

			muteButArr = muteButArr.add(muteButton);
			spaceButArr = spaceButArr.add(spaceButton);
			soloButArr = soloButArr.add(soloButton);
			recButArr = recButArr.add(recButton);

			buttonsLay1 = HLayout(*[muteButton, spaceButton]);
			buttonsLay2 = HLayout(*[soloButton, recButton]);

			popupmenusize = 14;

			if((1..3).includes(item), {
				slotsSize = 68;
			}, {
				slotsSize = 68+(16*(item-3));
			});

			slotsSizeArr = slotsSizeArr.add(slotsSize);

			//output label
			outputLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			outputLabel.font = basicFont; outputLabel.string_("Output");

			if(index == (sysChans.size-1), {
				//master output button
				outputMenu = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
				outputMenu.canFocus = false;
				outputMenu.states_([["", Color.white, Color.black]])
				.font_(basicFont);
				outputMenu.string = "Outputs";
				outputMenu.srcAction = {|it|
					var boundArr, scrollPoint, scrollOrg;
					boundArr = it.bounds.asArray;
					scrollPoint = mixerWin.visibleOrigin;
					scrollOrg = scrollPoint.asArray;
					this.mastOutGUI(boundArr, scrollOrg);
				};
			}, {
				//output menu
				outputMenu = PopUpMenu().maxHeight_(popupmenusize)
				.minHeight_(popupmenusize).minWidth_(slotsSize).maxWidth_(slotsSize);
				outputMenu.items = ["", "master"];
				if(outputSettings[index] == 'master', {
					outputMenu.value = 1;
				}, {
					outputMenu.value = 0;
				});
				outputMenu.background_(Color.black).stringColor_(Color.white)
				.font_(basicFont);
			});
			outputMenuArr = outputMenuArr.add(outputMenu);

			//sends label
			if(index == (sysChans.size-1), {
				sendsString = "Recording";
			}, {
				sendsString = "Sends";
			});
			sendsLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			sendsLabel.font = basicFont; sendsLabel.string_(sendsString);

			//sends menu
			if( index != (sysChans.size-1), {
				sends.do{var smenu, sknob, sendItems;
					smenu = PopUpMenu().maxHeight_(popupmenusize).minHeight_(popupmenusize)
					.maxWidth_(slotsSize-popupmenusize);

					sendItems = mixTrackNames.select({|item|
						item.asString.find("bus").notNil }).collect{|it|
						it.asString.divNumStr[1] };
					//offer the next free bus number (bus1 when there are no buses yet)
					sendItems = sendItems ++ ((sendItems.maxItem ? 0) + 1);

					smenu.items = [""] ++ sendItems.collect({|item| "bus" ++ item });

					smenu.background_(Color.black).stringColor_(Color.white)
					.font_(basicFont);
					sknob = Knob().minWidth_(popupmenusize).maxWidth_(popupmenusize)
					.maxHeight_(popupmenusize).minHeight_(popupmenusize);
					sknob.color = knobColors;

					sendsMenu = sendsMenu.add(smenu);
					sendsKnobs = sendsKnobs.add(sknob);
					sendsLay = sendsLay.add(HLayout(smenu, sknob));
				};
				sendsMenuArr = sendsMenuArr.add(sendsMenu);
				sendsKnobArr = sendsKnobArr.add(sendsKnobs);
			}, {
				sends.do{|sendInd|
					var smenu;
					smenu = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
					.minWidth_(slotsSize).maxWidth_(slotsSize);
					smenu.canFocus = false;
					if(sendInd == 0, {
						smenu.states_([["prepare rec", Color.red, Color.black],
							["record", Color.white, Color.red],
							["stop record", Color.red, Color.white]]);
						recordingButton = smenu;
						smenu.srcAction = {|butt|
							var value;
							value = butt.value;
							case
							{value == 1} {
								this.prepareRecording;
							}
							{value == 2} {
								this.startRecording;
							}
							{value == 0} {
								this.stopRecording;
							};
							recordingValBut = (value+1)%3;
						};
						if(recordingValBut.notNil, {
							recordingButton.value = (recordingValBut-1)%3;
						});
					}, {
						smenu.states_([["dir in rec", Color.white, Color.black]]);
						smenu.srcAction = {|it|
							var boundArr, scrollPoint, scrollOrg;
							boundArr = it.bounds.asArray;
							scrollPoint = mixerWin.visibleOrigin;
							scrollOrg = scrollPoint.asArray;
							this.dirInRec(boundArr, scrollOrg);
						};
					});
					smenu.font_(basicFont);
					if(sendInd < 2, {
						sendsLay = sendsLay.add(smenu);
					}, {
						sendsLay = sendsLay.add(nil);
					});
				};
			});
			//audio fxs
			fxLabel = StaticText(canvas).align_(\center).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			fxLabel.font = basicFont; fxLabel.string_("Audio FX");
			//fx buttons
			fxsNum.do{var fxbutton;
				fxbutton = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
				fxbutton.canFocus = false;
				fxbutton.states_([["", Color.white, Color.black]])
				.font_(basicFont);
				fxSlot = fxSlot.add(fxbutton);
			};
			fxSlotArr = fxSlotArr.add(fxSlot);
			//input label
			inputLabel = StaticText(canvas).align_(\right).background_(Color.black)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10)
			.maxWidth_(46).minWidth_(46);
			inputLabel.font = basicFont; inputLabel.string_("Input");
			//trim knob
			inKnob = Knob().minWidth_(popupmenusize).maxWidth_(popupmenusize)
			.maxHeight_(popupmenusize).minHeight_(popupmenusize);
			inKnob.color = knobColors;
			inKnob.centered_(true).srcAction = {|knob| var thisKey, thisSpec;
				thisKey = ("in" ++ mixTrackNames[index].asString.capitalise).asSymbol;
				thisSpec = this.getSpec(thisKey, \trim).asSpec;
				("Ndef(" ++ thisKey.cs ++ ").set('trim', " ++
					thisSpec.map(knob.value) ++ ");").radcode(\radpostcont);
			};
			inKnobArr = inKnobArr.add(inKnob);

			inputLabelArr = HLayout(*[ [inputLabel, align: \right], [inKnob, align: \right] ]);
			//input menu
			inputMenu = PopUpMenu().maxHeight_(popupmenusize)
			.minHeight_(popupmenusize).minWidth_(slotsSize).maxWidth_(slotsSize);

			this.inputLablesFunc(index, mixTrackNames, inputMenu);

			inputMenu.background_(Color.black).stringColor_(Color.white)
			.font_(basicFont).srcAction = { arg menu; var arr, trackInInf;
				trackInInf = 	mixTrackNames[index].asString.divNumStr;

				this.labelsToInFunc(trackInInf, menu.item);

			};

			inputMenuArr = inputMenuArr.add(inputMenu);

			//source button (tracks only): shows the track's source and opens its window
			if(mixTrackNames[index].asString.find("track").notNil, {
				srcButton = Button().maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
				srcButton.canFocus = false;
				srcButton.states_([["", Color.white, Color.black]])
				.font_(basicFont);
				srcButton.string = this.prSourceLabel(index);
				srcButton.srcAction = {|butt|
					var scrollOrg;
					scrollOrg = mixerWin.visibleOrigin.asArray;
					this.openTrackSource(mixTrackNames[index].asString.divNumStr[1] ? 1,
						butt.bounds.left+mixerWin.bounds.left-scrollOrg[0],
						(mixerWin.bounds.top-45)+butt.bounds.top+68);
				};
			}, {
				srcButton = StaticText(canvas).background_(Color.black)
				.maxHeight_(popupmenusize).minHeight_(popupmenusize)
				.minWidth_(slotsSize).maxWidth_(slotsSize);
			});
			srcButtonArr = srcButtonArr.add(srcButton);

			setInputMenu = {|indMenu, valMenu|
				inputMenuArr[indMenu].value = valMenu;
			};

			sliderArr = sliderArr.add(slider);
			levelArr = levelArr.add(level);
			ts = [slider] ++ level;
			thisLay = HLayout(*ts);
			//track name label
			case
			{mixTrackNames[index].asString.find("track").notNil} {
				trackColor = colorTrack}
			{mixTrackNames[index].asString.find("bus").notNil} {
				trackColor = colorBus}
			{mixTrackNames[index].asString.find("master").notNil} {
				trackColor = colorMaster};
			trackLabel = StaticText(canvas).align_(\center).background_(trackColor)
			.stringColor_(Color.white).maxHeight_(10).minHeight_(10);
			trackLabel.font = basicFont;
			trackLabel.string_(mixTrackNames[index].asString.capitalise);
			trackLabel.minWidth_(slotsSize).maxWidth_(slotsSize);
			trackLabelArr = trackLabelArr.add(trackLabel);

			//input
			[[inputLabelArr, align: \bottom], [inputMenu, align: \bottom],
				[srcButton, align: \bottom]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//audio fx
			finalLayout = finalLayout.add([fxLabel, align: \center]);
			fxSlot.do{|thisSlot|
				finalLayout = finalLayout.add([thisSlot, align: \center]);
			};
			//sends
			finalLayout = finalLayout.add([sendsLabel, align: \bottom]);
			sendsLay.do{|smenu|
				finalLayout = finalLayout.add([smenu, align: \bottom]);
			};
			//output
			[[outputLabel, align: \bottom], [outputMenu, align: \bottom]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//spatialisation interface
			[[spaceTextLay, align: \bottom], [panKnobLay, align: \bottom]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//slider and levelIndicators
			[[hlay, align: \center], [thisLay, align: \center]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//button UI solo,mute,rec,space
			[[buttonsLay1, align: \center], [buttonsLay2, align: \center]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			//track names
			finalLayout = finalLayout.add([trackLabel, align: \below]);
			vlay = vlay.add(VLayout(*finalLayout) );
		};

		//setting up interface
		sliderTextArr.do{|item, index| item.font = basicFont; item.string_(
			mixTrackNdefs[index].getKeysValues.collect({|item|
				if(item[0] == \volume, {item[1]});
		})[0].round(0.1).asString);
		};
		levelTextArr.do{|item, index| item.font = basicFont; item.string_("-inf");
			item.mouseDownAction_({
				peakMax[index] = -inf;
				levelTextArr[index].stringColor = Color.white;
				item.string_("-inf");
			});
		};
		peakMax = -inf!levelTextArr.size;
		//panning
		/*panKnobTextArr.flat.do{|item| if(item.notNil, {item.font = basicFont});};*/
		panKnobTextArr.flat.do{|item| if(item.notNil, {
			case
			{item.bounds.width >= 24} { item.font = basicFont }
			{item.bounds.width == 19} { item.font = Font("Monaco", 7); }
			{item.bounds.width <= 14} { item.font =  Font("Monaco", 6); };

		});};
		/*panSpec = \pan.asSpec;*/
		panKnobArr.do{|item, index|
			var panKey, getKeyValues, panKeyValues, panValues, hidNodes, hidPan, panSpec, spaceSpecs;
			panKey = ("space" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			getKeyValues = Ndef(panKey).getKeysValues;

			spaceSpecs = specs.collect{|item| item.last };
			panSpec = spaceSpecs.flop[1][spaceSpecs.flop[0].indexOfEqual(panKey)];
			/*panSpec = specs.flop[2].flop[1][specs.flop[2].flop[0].indexOfEqual(panKey)];*/
			/*panSpec = \pan.asSpec;*/
			hidNodes = HIDMap.hidNodes;
			if(getKeyValues.flop[0].includes(\dirmul).not, {
				/*panKeyValues = getKeyValues.flat;
				case
				{panKeyValues.size == 0} {panValues = 0}*/
				/*{panKeyValues.includes(\pan)} {
					panValues = panKeyValues[panKeyValues.indexOf(\pan)+1]}
				{panKeyValues.includes(\panx)} {
					panValues = [panKeyValues[panKeyValues.indexOf(\panx)+1],
						panKeyValues[panKeyValues.indexOf(\pany)+1]
				]};*/
					//this needs work
				/*"item".postln;
				item.postln;*/
					item.do{|it, ind|
						/*it.postln;
						getKeyValues[ind].postln;*/
						panValues = getKeyValues[ind][1];
					/*panValues.postln;*/

					if(hidNodes.notNil, {
				if(hidNodes.notEmpty, {
							/*Ndef(panKey).postln;
							hidNodes.postln;
							getKeyValues[ind][0].postln;*/
					/*		[ [ 'hid1', Ndef('spaceTrack1'), pan, 0 ] ]*/
							hidPan = (hidNodes.flop[1].includes(Ndef(panKey)))
							.and(hidNodes.flop[2].includes(getKeyValues[ind][0]));
				}, {hidPan = false});
			}, {hidPan = false});

						if((panValues.cs.find("mod").notNil).or(hidPan), {
							it.background = colorCritical;
							it.enabled = false;
							/*panValues = panSpec[ind][1].asSpec.map(0.5);*/
						it.value = 0.5;
						panKnobTextArr[index][ind].string_("mod");
						}, {
					it.value = panSpec[ind][1].asSpec.unmap(panValues);
						panKnobTextArr[index][ind].string_(panValues);
					});
						it.srcAction = {|val|
							var newPanVal;
						newPanVal = panSpec[ind][1].asSpec.map(val.value);
						panKnobTextArr[index][ind].string_(newPanVal.round(0.1).asString);
							("Ndef(" ++ panKey.cs ++ ").set(" ++ getKeyValues[ind][0].cs ++ ", "
								++ newPanVal ++ ");").radcode(\radpostcont);
						};
					};
			});
		};

		sliderArr.do{|item, index|
			var volSpec, volValue, volNdef, hidVol, hidNodes;
			volSpec = this.getSpec(mixTrackNames[index], \volume).asSpec;
			volNdef = mixTrackNdefs[index];
			volValue = volNdef.getKeysValues.collect({|item|
				if(item[0] == \volume, {item[1]});
			})[0];
			hidNodes = HIDMap.hidNodes;
			if(hidNodes.notNil, {
				if(hidNodes.notEmpty, {
					hidVol = hidNodes.flop[1].includes(volNdef);
				}, {hidVol = false});
			}, {hidVol = false});
			if((volValue.cs.find("mod").notNil).or(hidVol), {
				item.background = colorCritical;
				item.enabled = false;
				volValue = 0;
				sliderTextArr[index].string_(volValue);
			});
			item.value_(
				volSpec.unmap( volValue );
			).srcAction_({|val|
				sliderTextArr[index].string_(volSpec.map(val.value).round(0.1).asString);
				(mixTrackNdefs[index].cs ++ ".set('volume', " ++
					volSpec.map(val.value) ++ ");").radcode(\radpostcont);
		});  };

		setVolSlider = {|index, value|
			var volSpec;
			volSpec = this.getSpec(mixTrackNames[index], \volume).asSpec;
			sliderArr[index].value = volSpec.unmap(value);
			sliderTextArr[index].string_(value.round(0.1).asString);
		};

		setPanKnob = {|index, value, ind=0|
			var panKey, getKeyValues, panSpec, spaceSpecs;
			panKey = ("space" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			getKeyValues = Ndef(panKey).getKeysValues;
			//the space spec is always the last entry of a strip, whatever effects it has
			spaceSpecs = specs.collect{|item| item.last };
			panSpec = spaceSpecs.flop[1][spaceSpecs.flop[0].indexOfEqual(panKey)];
			panKnobArr[index][ind].value = panSpec[ind][1].asSpec.unmap(value);
			panKnobTextArr[index][ind].string_(value);
		};

		setInKnob = {|index, value|
			var trimSpec, trimKey;
			trimKey = ("in" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			trimSpec = this.getSpec(trimKey, \trim).asSpec;
			inKnobArr[index].value = trimSpec.unmap(value);
		};

		levelArr.do{|it| it.do{|item|
			item.meterColor = colorMeter;
			item.warningColor = colorWarning;
			item.criticalColor = colorCritical;
			item.drawsPeak = true;
		}
		};

		canvas.layout = HLayout(*vlay);
		mixerWin.canvas = canvas;
		spaceGap = sysChans.collect({|item|
			if((1..3).includes(item), {
				item =7;
			}, {
				item = 6;
			});
		});

		gapHeight = 6;
		sumHeight = panKnobArr.flat.collect{|item|
			/*if(item.notNil, {item.bounds.height}, {30});*/
			if(item.notNil, {30}, {30});
		}.maxItem + gapHeight;
		sumHeight = 45 + 9 + sumHeight + (7 * (10+gapHeight)) + (2 * (12+gapHeight)) +
		(180+gapHeight) + (sendsMenuArr[0].size*(14+gapHeight)) +
		(fxSlotArr[0].size*(14+gapHeight)) + (14+gapHeight);
		sumHeight = sumHeight + 4;

		/*slotsSizeArr.postln;*/


		sumWidth = 9 + slotsSizeArr.sum + spaceGap.sum + (9-spaceGap.last+2);
		//provisional fix for space interface
		/*panKnobTextArr.postln;*/
		/*panKnobTextArr.collect{|it| it.size }.maxIndex;*/
/*		panKnobTextArr.size.postln;
		sumWidth = sumWidth + (20 * panKnobTextArr.collect{|it| it.size }.maxItem; );*/
		if(sumWidth > screenBounds.width, {
			sumWidth = screenBounds.width;
		});
		if(sumHeight > (screenBounds.height-45), {
			sumHeight = screenBounds.height-45;
		});
		mixerWin.maxWidth_(sumWidth).minWidth_(sumWidth);
		mixerWin.maxHeight_(sumHeight).minHeight_(sumHeight);
		mixerWin.front;

		if(scrollPoint.notNil, {
			//check this bug hasn't been fixed in latest SC version, if so, remove fork
			{0.001.yield; mixerWin.visibleOrigin_(scrollPoint);}.fork(AppClock);
		});

		//setting busIns
		if(busInSettings.isNil, {
			busInSettings = (nil!(sendsMenuArr.flat.size-1)).reshapeLike(sendsMenuArr);
		});

		knobFunc = {|it, trackLb, thisKey|
			var thisNdefVal, selArg, thisSpec, selValue, hidKnob, hidNodes;
			if(thisKey.notNil, {
				thisNdefVal = Ndef(thisKey).getKeysValues;
				selArg = ("vol" ++ (busArr.flop[1][busArr.flop[0].indexOf(thisKey)].collect{|keyVal|
					keyVal.key}.indexOf(trackLb) + 1)).asSymbol;
				thisSpec = this.getSpec(thisKey.asString.replace("In", "").asSymbol, \volume).asSpec;
				selValue = thisNdefVal.flop[1][thisNdefVal.flop[0].indexOf(selArg)];
				hidNodes = HIDMap.hidNodes;
				if(hidNodes.notNil, {
					if(hidNodes.notEmpty, {
						hidKnob = hidNodes.flop[1].includes(Ndef(thisKey));
					}, { hidKnob = false });
				}, {hidKnob = false});
				if((selValue.cs.find("mod").notNil).or(hidKnob), {
					it.background = colorCritical;
					it.enabled = false;
					selValue = -inf;
				});
				it.value = thisSpec.unmap( selValue );
				it.srcAction = {|val|
					("Ndef(" ++ thisKey.cs ++ ").set(" ++ selArg.cs ++
						", " ++ thisSpec.map(val.value) ++ ");").radcode(\radpostcont);
				};
			});
		};

		if(busInLabels.notNil, {
			busInLabels.do{|item|
				var thisTackNameInd;
				thisTackNameInd = mixTrackNames.indexOf(item[0]);
				if(thisTackNameInd.notNil, {
					sendsMenuArr[thisTackNameInd].do{|it, ind|
						var labInArr;
						if(item[1][ind].notNil, {
							it.value = it.items.collect({|it| it.divNumStr[1] })
							.indexOf(item[1][ind].asString.divNumStr[1]);
							busInSettings[thisTackNameInd][ind] = it.item;
						});
					};
					sendsKnobArr[mixTrackNames.indexOf(item[0])].do{|it, ind|
						knobFunc.(it, item[0], item[1][ind]);
					};
				});
			};
		});

		inKnobArr.do{|item, index|
			var thisKey, trimKeys, thisSpec, thisTrimKey, hidNodes, hidTrim;
			thisKey = ("in" ++ mixTrackNames[index].asString.capitalise).asSymbol;
			trimKeys = Ndef(thisKey).controlKeysValues;
			thisSpec = this.getSpec(thisKey, \trim).asSpec;

			hidNodes = HIDMap.hidNodes;
			if(hidNodes.notNil, {
				if(hidNodes.notEmpty, {
					hidTrim = hidNodes.flop[1].includes(Ndef(thisKey));
				}, {hidTrim = false});
			}, {hidTrim = false});

			if((trimKeys.notEmpty).or(hidTrim), {
				thisTrimKey = trimKeys[trimKeys.indexOf(\trim)+1];
				if((thisTrimKey.cs.find("mod").notNil).or(hidTrim), {
					item.background = colorCritical;
					item.enabled = false;
					thisTrimKey = 0;
				});
				item.value = thisSpec.unmap( thisTrimKey );
			}, {
				item.value = 0.5;
			});
		};

		setBusIns = {arg trackInd=0, slotInd=0, busInNum=1;
			var thisMenu, thisString;
			thisMenu = sendsMenuArr[trackInd][slotInd];
			if(busInNum == 0, {
				thisString = "";
			}, {
				thisString = ("bus" ++ busInNum).asString;
			});
			thisMenu.valueAction = thisMenu.items.indexOfEqual(thisString);
		};

		setKnobIns = {arg trackInd=0, slotInd=0, value=0;
			var thisMenu, thisString, thisSpec;
			thisMenu = sendsKnobArr[trackInd][slotInd];
			thisSpec = this.getSpec(mixTrackNames[trackInd], \volume).asSpec;
			thisMenu.value = thisSpec.unmap(value);
		};

		sendsMenuArr.do{|item, index|
			item.do{|it, ind|
				it.srcAction = {arg menu;
					var thisBusItem, thisTrackLabel, thisBusNum, busInBool1, busInBool2;

					scrollPoint = mixerWin.visibleOrigin;

					thisBusNum = menu.item.divNumStr[1];
					thisTrackLabel = mixTrackNames[index].asString.divNumStr[0].asSymbol;

					if(([''] ++ mixTrackNames).includesEqual(it.item.asSymbol).not, {
						this.autoAddTrack(\bus, systemChanNum, action: {
							{var trackorbusIndex;
								trackorbusIndex = mixTrackNames[index].asString.divNumStr[1];
								this.setSend(thisTrackLabel, trackorbusIndex, ind+1, thisBusNum);
							}.defer;
						});
					}, {
						thisBusItem = busInSettings[index][ind];
						thisTrackLabel = mixTrackNames[index].asString.divNumStr;
						if(menu.item == "", {
							busInSettings[index][ind] = nil;
						}, {
							busInSettings[index][ind] = menu.item;
						});
						/*busInSettings[index][ind] = menu.item;*/
						busInBool1 = busInSettings[index].select({|item, index| index != ind})
						.includesEqual(it.item);
						busInSettings[index].collect({|item|
							if(item.notNil, {item.asSymbol}, {item});
						});
						busInBool2 =  busInSettings[index].collect({|item|
							if(item.notNil, {item.asSymbol}, {item});
						}).includes(outputSettings[index]);
						if((busInBool1).or(busInBool2), {
							"This bus is already assigned to this track".warn;
							sendsMenuArr[index][ind].value = 0;
							sendsKnobArr[index][ind].value = 0;
							sendsKnobArr[index][ind].action = {};
						}, {
							if(((thisBusItem.isNil).or(thisBusItem == "")).not, {
								if(busInSettings[index].includesEqual(thisBusItem).not, {
									//remove
									thisBusNum = thisBusItem.divNumStr[1];
									this.removeBus(thisTrackLabel[1], thisBusNum,
										thisTrackLabel[0].asSymbol);
								});
							});
							if(menu.item != "", {
								thisBusNum = menu.item.divNumStr[1];
								this.bus(thisTrackLabel[1], thisBusNum, -inf,
									thisTrackLabel[0].asSymbol, {
										{knobFunc.(sendsKnobArr[index][ind],
											mixTrackNames[index],
											("busIn" ++ thisBusNum).asSymbol);
										if(ind == (sends-1), { this.refreshMixGUI; });
										}.defer;
								});
							}, {
								sendsKnobArr[index][ind].value = 0;
								sendsKnobArr[index][ind].action = {};
								if(ind == (sends-2), { this.refreshMixGUI; });
							});
						});
					});
				};
			};
		};
		//fx UI settings
		fxSlotArr.do{|item, index|
			item.do{|it, ind|
				var trackInfoInt, trackInfoArr, thisFltInfo, tracksFlt, thisFltTags, fltTagArr, thisSlotInfo,
				fltWinFunc1, fltWinFunc2;

				trackInfoArr = mixTrackNames[index].asString.divNumStr;
				if(trackInfoArr[1] == nil, {trackInfoArr[1] = 1});

				if(filters.notNil, {
					if(filters.notEmpty, {
						thisFltTags = filters.flop[0].collect({|item| this.convFilterTag(item) });
						fltTagArr = thisFltTags.collect({|item|
							[item[0], item[1].asInteger, item[2].asInteger] });
						thisFltInfo = thisFltTags.collect({|item| [item[0], item[1].asInteger] });
						tracksFlt = ([thisFltInfo.flop[0], thisFltInfo.flop[1]].flop);
						trackInfoInt = [trackInfoArr[0].asSymbol, trackInfoArr[1]];
						if(trackInfoInt[1].isNil, {trackInfoInt[1] = 1 });
						if(tracksFlt.collect({ |item|
							(item == trackInfoInt) }).includes(true), {
							thisSlotInfo = [trackInfoInt, ind+1].flat;
							if(fltTagArr.collect({|item| item == thisSlotInfo}).includes(true), {
								it.string = filters[fltTagArr.indexOfEqual(thisSlotInfo)][1];
							});
						});
					});
				});
				fltWinFunc1 = {arg menu, labelKey;
					{
						menu.string = labelKey;
						fltMenuWindow.close;
						fltMenuWindow = nil;
						if((ind+1) > (fxsNum-1), {
							server.sync; this.refreshMixGUI;
						});
					}.fork(AppClock);
				};
				fltWinFunc2 = {arg menu, thisListView, irItems, labelKey;
					var dash;
					menu.string = labelKey;
					dash = "/";
					Platform.case(
						\windows,   {dash = "\\"; }
					);
					irItems = PathName(soundFilePath ++ dash ++ "IR" ++ dash).entries
					.collect({|item| item.fileNameWithoutExtension });
					thisListView.items = [""] ++ irItems;
					thisListView.srcAction = {|sbs|
						this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
							labelKey, data: [\convrev, sbs.item.asSymbol, 2048], action: {
								{
									fltMenuWindow.close;
									fltMenuWindow = nil;
									if((ind+1) > (fxsNum-1), {
										server.sync; this.refreshMixGUI
									});
								}.fork(AppClock);
						});
					};
				};
				it.srcMouseDownAction = { arg menu;
					var boundArr, thisBounds, thisArrBounds, thisitemArr,
					thisListView, thisTagFlt, scrollOrg, fltUIArr;
					boundArr = it.bounds.asArray;
					scrollPoint = mixerWin.visibleOrigin;
					scrollOrg = scrollPoint.asArray;
					thisBounds = 	Rect(boundArr[0]+mixerWin.bounds.left - scrollOrg[0],
						(screenBounds.height-boundArr[1]-285)-(mixerWin.bounds.top-45),
						140, 240);
					thisArrBounds = thisBounds.asArray;
					if(menu.string == "", {
						fltMenuWindow = Window.new("", thisBounds, border: false).front;
						fltMenuWindow.background_(Color.black);
						thisitemArr = ([""] ++ SynthFile.read(\filter, exclude: excludeLibs).sort );
						thisListView = ListView(fltMenuWindow,Rect(0,0,(thisArrBounds[2]),
							(thisArrBounds[3])))
						.items_(thisitemArr)
						.background_(Color.clear)
						.font_(Font("Monaco", 10);)
						.stringColor_(Color.white)
						.hiliteColor_(colorMeter;)
						.srcAction_({ arg sbs;
							var labelKey, irItems;
							labelKey = thisListView.items[sbs.value];
							if(labelKey.asString.find("convrev").isNil, {
								this.filter(trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1,
									labelKey, action: {
										fltWinFunc1.(menu, labelKey);
								});
							}, {
								fltWinFunc2.(menu, thisListView, irItems, labelKey);
							});
						});
					}, {
						fltUIArr = [trackInfoArr[0].asSymbol, trackInfoArr[1], ind+1];
						if(filters.notNil, {
							thisTagFlt = this.findFilterTag(fltUIArr[0],
								fltUIArr[1], fltUIArr[2]);
							if(filters.flop[0].includes(thisTagFlt).not, {
								this.filter(fltUIArr[0], fltUIArr[1], fltUIArr[2],
									menu.string.asSymbol);
							});
						});
						this.filterGUI(fltUIArr[0], fltUIArr[1], fltUIArr[2],
							[menu.bounds.left+mixerWin.bounds.left-scrollPoint.asArray[0],
								(mixerWin.bounds.top-45)+menu.bounds.top+68], menu);
					});
				};
			}
		};

		outputMenuArr.do{|it, ind|
			if(ind != (sysChans.size-1), {
				it.srcAction = {arg menu;
					outputSettings[ind] = menu.item.asSymbol;
					this.outputMasterFunc;
				};
			});
		};

		setOutputMenu = {arg index, value;
			outputMenuArr[index].value = value;
		};

		Ndef('AssemblageGUI', {
			var in, imp;
			in = sysChans.collect({|item, index|
				if(item == 1, {
					ArrayMax.ar(Ndef(("space" ++ mixTrackNames[index].asString.capitalise)
						.asSymbol).ar)[0];
				}, {
					Ndef(("space" ++ mixTrackNames[index].asString.capitalise).asSymbol).ar;
				});
			});
			in = in.flat;
			imp = Impulse.ar(updateFreq); //same rate the OSC handler assumes
			SendReply.ar(imp, '/AssemblageGUI',
				[
					RunningSum.ar(in.squared, server.sampleRate / updateFreq),
					Peak.ar(in, Delay1.ar(imp)).lag(0, 3)
				].flop.flat
			);
		});

		oscDefFunc = {
			OSCdef(\AssemblageGUI, {|msg, time, addr, recvPort|
				var dBLow, array, numRMSSampsRecip, numRMSSamps, peakVal, peakAmp, peakArr;
				numRMSSamps = server.sampleRate / updateFreq;
				numRMSSampsRecip = 1 / numRMSSamps;
				dBLow = -80;
				{
					try {
						peakArr = [];
						msg.copyToEnd(3).pairsDo({|val, peak, i|
							var meter, thisPeakVal, value;
							i = i * 0.5;
							meter = 	levelArr.flat[i];
							if(meter.notNil, {
								value = val*levelSoloStates[i];
								meter.value = (value.max(0.0) * numRMSSampsRecip)
								.sqrt.ampdb.linlin(dBLow, 0, 0, 1);
								peakVal = (peak*levelSoloStates[i]).ampdb;
								thisPeakVal = peakVal.linlin(dBLow, 0, 0, 1);
								meter.peakLevel = thisPeakVal;
								peakArr = peakArr.add(peakVal);
							});
						});
						if(peakArr.notNil, {
							peakArr = peakArr.reshapeLike(levelArr);
							peakArr.do{|item, index|
								var peakDb;
								peakDb = item.maxItem;
								if(peakDb < dBLow, {peakDb = -inf });

								if((levelSoloStates.atAll((sysChans.integrate - sysChans[0]))[index] == 0).or(
									muteStates[index] == 1;
								), {
									levelTextArr[index].stringColor = Color.white;
									levelTextArr[index].string = "-inf";
									peakMax[index] = -inf;
								}, {
									if(peakMax[index] < peakDb, {
										peakAmp = peakDb.linlin(dBLow, 0, 0, 1);
										if(peakAmp >= 0.9999, {
											if(levelTextArr[index].notNil, {
												levelTextArr[index].stringColor_(colorCritical);
											});
										}, {
											if(levelTextArr[index].notNil, {
												levelTextArr[index].stringColor_(Color.white);
											});
										});
										levelTextArr[index].string = peakDb.round(0.1).asString;
										peakMax[index] = peakDb;
									});
								});
							};
						});
					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
			}, \AssemblageGUI);
		};

		oscDefFunc.();

		mixerWin.onClose = {
			/*levelTextArr = nil;*/
			/*mixerWin = nil;*/

			OSCdef(\AssemblageGUI).free;
			{
				nodeTime.yield;
				if(mixerWin.visible.isNil, {
					Ndef('AssemblageGUI').clear;
				});
			}.fork(AppClock);

			levelTextArr = nil;

			filtersWindow.do{|item| item.close; };
			if(fltMenuWindow.notNil, {
				if(fltMenuWindow.visible, {
					fltMenuWindow.close;
					fltMenuWindow = nil;
				});
			});
		};

	}

	refreshMixGUI {
		if(mixerWin.notNil, {
			mixerWin.children.do { |child| child.remove };
			winRefresh = true;
		});
		this.mixGUI;
	}

	mixer {
		this.refreshMixGUI;
	}

	inputLablesFunc {arg ind, thisTrackNames, thisInputMenu;
		var mixInputLabelArr, mixInputLabels, outputFunc;

		if(thisTrackNames[ind].asString.find("track").notNil, {
			mixInputLabels = (("in" ++
				thisTrackNames[ind].asString.capitalise).asSymbol);
			if(inputs.flop[0].includes(mixInputLabels), {
				mixInputLabels = inputs.flop[1][inputs.flop[0].indexOfEqual(mixInputLabels)];
				if(mixInputLabels.isArray, {
					mixInputLabels = mixInputLabels.collect({|item| item.key });
				}, {
					mixInputLabels = mixInputLabels.key;
				});
				mixInputLabelArr = [mixInputLabels.asString]
				++ Block.ndefs.collect({|item| item.key.asString});
				mixInputLabelArr = mixInputLabelArr.rejectSame.sort;
				if(thisInputMenu.notNil, {
					thisInputMenu.items = mixInputLabelArr;
					thisInputMenu.value = mixInputLabelArr.indexOfEqual(mixInputLabels.asString);
				});
				outputFunc = [mixInputLabelArr, mixInputLabelArr.indexOfEqual(mixInputLabels.asString)];

			});
		}, {
			if(thisInputMenu.notNil, {
				thisInputMenu.items = [thisTrackNames[ind]];
			});
			outputFunc = [thisTrackNames[ind]];
		});
		^outputFunc;
	}

	labelsToInFunc {arg trackInInf, menuItem;
		var arr;
		if(menuItem.includesString("["), {
			arr = menuItem.replace("[", "").replace("]", "").replace(" ", "").split($,);
			arr = arr.collect{|it| Ndef(it.asSymbol) };
			this.input(arr, trackInInf[0].asSymbol, trackInInf[1]);
		}, {
			this.input(Ndef(menuItem.asSymbol), trackInInf[0].asSymbol, trackInInf[1]);
		});
	}

	refreshFunc {
		if(mixerWin.notNil, {
			if(mixerWin.visible.notNil, {
				this.refreshMixGUI;
			});
		});
	}

	filterWinGUI {arg filterTag=\filterTrack_1_1, filterKey=\pch, filterPairs,
		fltWinLeft=0, fltWinDown=0, mixButton;
		var winName, filtersWin, winBool;
		winName = filterTag.asString;
		winName = winName ++ " | " ++ filterKey.asString;
		winBool = true;
		if(filtersWindow.notNil, {
			if(filtersWindow.collect({|item| item.name ==  winName}).includes(true), {
				winBool = false;
			});
		});
		if(winBool, {
			filtersWin = this.prArgWinGUI(filterTag, winName, filterPairs,
				SpecFile.read(\filter, filterKey, false), fltWinLeft, fltWinDown,
				"R E M O V E   F I L T E R", {|win|
					var filterInfoArr, fxsNum2;
					filterInfoArr = this.convFilterTag(filterTag);
					this.removeFilter(filterInfoArr[0], filterInfoArr[1].asInteger, filterInfoArr[2].asInteger, {
						win.close;
						if(mixButton.notNil, {
							mixButton.string = "";
						});
						if(filters.notNil, {
							if(filters.notEmpty, {
								fxsNum2 = filters.flop[0].collect({|item|
									item.asString.split($_).last.asInteger }).maxItem.max(1) + 1;
								{
									if(fxsNum2 < fxsNum, {
										server.sync; this.refreshMixGUI;
									});
								}.fork(AppClock);
							});
						});
					});
				});
			filtersWindow = filtersWindow.add(filtersWin);
			filtersWin.onClose = {
				filtersWindow = filtersWindow.reject({|item| item.name == filtersWin.name });
			};
		}, {
			filtersWin = filtersWindow.collect({|item|
				item.name });
			filtersWindow[filtersWin.indexOfEqual(winName)].front;
		});
	}

	// An argument window for an Ndef, shared by the effect windows and the
	// source windows: a slider for each argument with a spec (specArr, one entry
	// per argument, or nil), a text field otherwise; arguments that are modulated
	// or mapped to a controller are greyed out. The button at the top shows
	// buttonString and calls buttonAction with the window. Returns the window.
	prArgWinGUI {arg ndefKey, winName, pairs, specArr, winLeft=0, winDown=0,
		buttonString, buttonAction;
		var filtersWin, fltCanvas, fxKnobTextArr, fltVlay, fltWinWidth, fltWinHeight,
		stringLengh, argArr, defaultArgArr, specBool, removeButton, fltWinTop;
		argArr = pairs.flop[0];
		defaultArgArr = pairs.flop[1];
		stringLengh = argArr.collect({|item| item.asString.size }).maxItem*4.8;
		filtersWin = ScrollView()
		.name_(winName);
		filtersWin.hasHorizontalScroller = false;
		fltWinWidth = (250) + stringLengh + 7;
		fltWinTop = screenBounds.height-filtersWin.bounds.top-winDown;
		fltWinHeight = ( ((argArr.size+1) * (15 + 7)) + 13 + 6 ).min(fltWinTop);
		filtersWin.fixedHeight = fltWinHeight;
		filtersWin.fixedWidth = fltWinWidth;
		/*filtersWin.alwaysOnTop = true;*/
		fltCanvas = View();
		fltCanvas.background_(Color.black);
		argArr.do{|item, index|
			var finalLayout, fxKnob, fxKnobText, specOptions,
			fxKnobArr, labelText, labelString, defaultVal, thisSpec, thisFunc,
			thisResult, labelTextArr, hidFx, hidNodes, hidIndex;
			if(specArr.notNil, {
				specBool = specArr[index].notNil;
			}, {
				specBool = false;
			});
			fxKnobText = StaticText(fltCanvas).align_(\center)
			.background_(Color.black)
			.stringColor_(Color.white)
			.font_(basicFont)
			.minWidth_(40).maxWidth_(40).maxHeight_(10).minHeight_(10);
			hidNodes = HIDMap.hidNodes;
			if(hidNodes.notNil, {
				if(hidNodes.notEmpty, {
					hidIndex = hidNodes.flop[2].indexOfEqual(argArr[index]);
					hidFx = (hidNodes.flop[1].includes(Ndef(ndefKey)))
					.and(hidIndex.notNil);
				}, {hidFx = false});
			}, {hidFx = false});
			if(specBool, {
				thisSpec = specArr[index][1].asSpec;
				thisFunc = specArr[index][2];
				fxKnob = Slider().minWidth_(180).maxWidth_(180)
				.maxHeight_(15).minHeight_(15);
				fxKnob.orientation = \horizontal;

				if((defaultArgArr[index].isNumber).and(hidFx.not), {
					fxKnob.srcAction = {
						if(thisFunc.notNil, {
							thisResult = thisFunc.(thisSpec.map(fxKnob.value));
						}, {
							thisResult = thisSpec.map(fxKnob.value);
						});
						fxKnobText.string = thisResult.asString.copyRange(0, 7);
						("Ndef(" ++ ndefKey.cs ++ ").set(" ++ item.cs ++ ", "
							++ thisResult ++ ");").radcode(\radpostcont);
					};
					thisResult = Radicles.specUnmap(defaultArgArr[index], thisSpec, thisFunc);
					fxKnob.value = thisResult;
					fxKnobText.string = defaultArgArr[index].asString.copyRange(0, 7);
				}, {
					fxKnob.value = 0.5;
					fxKnob.background = colorCritical; //mod color
					fxKnob.enabled = false;
					if(hidFx, {
						fxKnobText.string = hidNodes.flop[0][hidIndex].asString.copyRange(0, 7);
					}, {
						fxKnobText.string = defaultArgArr[index].key.asString.copyRange(0, 7);
					});
				});
			}, {
				fxKnob = TextField().minWidth_(180).maxWidth_(180)
				.font_(basicFont)
				.background_(colorTextField)
				.maxHeight_(15).minHeight_(15);
				defaultVal = defaultArgArr[index];
				if(defaultVal.notNil, {
					if((defaultVal.cs.find("Ndef").isNil).and(hidFx.not), {
						fxKnob.string = defaultVal.cs;
						fxKnobText.string = defaultVal.cs.copyRange(0, 7);
						fxKnob.srcAction = {arg field;
							fxKnobText.string = field.value;
							("Ndef(" ++ ndefKey.cs ++ ").set(" ++ item.cs ++ ", "
								++ field.value ++ ");").radcode(\radpostcont);
						};
					}, {
						fxKnob.background = colorCritical; //mod color
						fxKnob.enabled = false;
						fxKnob.string = defaultVal.cs;
						if(hidFx, {
							fxKnobText.string = hidNodes.flop[0][hidIndex].asString.copyRange(0, 7);
						}, {
							fxKnobText.string = defaultVal.key.asString.copyRange(0, 7);
						});
					});
				});

			});
			labelString = item.asString;
			labelText = StaticText(fltCanvas).align_(\center)
			.background_(Color.black)
			.stringColor_(Color.white)
			.font_(basicFont)
			.string_(labelString)
			.maxWidth_(stringLengh)
			.minWidth_(stringLengh)
			.minHeight_(10);
			fxKnobTextArr = fxKnobTextArr.add(fxKnobText);
			labelTextArr = labelTextArr.add(labelText);
			fxKnobArr = fxKnobArr.add(fxKnob);
			[[labelText, align: \center], [fxKnob, align: \center],
				[fxKnobText, align: \center]].do{|lay|
				finalLayout = finalLayout.add(lay);
			};
			fltVlay = fltVlay.add(HLayout(*finalLayout) );
		};
		removeButton = Button().maxHeight_(15).minHeight_(15)
		.states_([["", colorCritical, Color.black]])
		.string_(buttonString)
		.font_(basicFont)
		.srcAction = { buttonAction.(filtersWin) };
		removeButton.canFocus = false;
		fltVlay = [removeButton] ++ fltVlay;
		fltCanvas.layout = VLayout(*fltVlay);
		filtersWin.canvas = fltCanvas;
		filtersWin.bounds = Rect(winLeft, winDown, fltWinWidth, fltWinHeight);
		filtersWin.front;
		^filtersWin;
	}
	filterGUIIndex {arg index=0, topLeftArr, mixButton;
		var filterTag, filterKey, filterPairs, filterInfo, top, left, thisFilter, defaultPos;
		if(filters.notNil, {
			thisFilter = filters[index];
			if(thisFilter.notNil, {
				filterKey = thisFilter[1];
				filterTag = thisFilter[0];
				filterInfo = this.convFilterTag(filterTag);
				filterPairs = this.getFilterPairs(filterInfo[0], filterInfo[1], filterInfo[2]);
				if(topLeftArr.isNil, {
					defaultPos = {top = 75; left = 75;};
					if(filtersWindow.isNil, {
						defaultPos.();
					}, {
						if(filtersWindow.isEmpty, {
							defaultPos.();
						}, {
							top = filtersWindow.last.bounds.top + 10;
							left = filtersWindow.last.bounds.left + 10;
						});
					});
				}, {
					left = topLeftArr[0];
					top = topLeftArr[1];
				});
				this.filterWinGUI(filterTag, filterKey, filterPairs, left, top, mixButton);
			}, {
				"Filter not found".warn;
			});
		}, {
			"No active filters".warn;
		});
	}

	filterGUI {arg type=\track, num= 1, slot=1, topLeftArr, mixButton;
		var filterTag, filterIndex;
		if(filters.notNil, {
			filterTag = this.findFilterTag(type, num, slot);
			filterIndex = filters.flop[0].indexOfEqual(filterTag);
			if(filterIndex.notNil, {
				this.filterGUIIndex(filterIndex, topLeftArr, mixButton);
			});
		}, {
			"No filters active".warn;
		});
	}

	mastOutGUI {arg boundArr, scrollOrg;
		var butt, oi1, oi2, selButt, oiArr, funcSource, thisBounds;
		oiIns = Ndef(\spaceMaster).numChannels;
		thisBounds = 	Rect(boundArr[0]+mixerWin.bounds.left - scrollOrg[0],
			(screenBounds.height-boundArr[1]-((oiOuts * 25+10)+45))-(mixerWin.bounds.top-45),
			(oiIns*25+10), (oiOuts * 25+10));
		mastOutWin = Window("", thisBounds, border:false);
		mastOutWin.background_(Color.black);
		butt = { {Button(mastOutWin).maxWidth_(15).maxHeight_(15)
			.states_([["", Color.white, Color.black], ["", Color.white, colorMaster] ]);
		} ! oiOuts } ! oiIns ;
		butt.flat.do{|item| item.canFocus = false };
		oiOuts.do{|ind|
			oi1 = oi1.add( StaticText(mastOutWin).string_(ind+1) );
		};
		oi1.do{|item| item.font = basicFont;
			item.stringColor_(Color.white);
			item.maxWidth_(15).maxHeight_(15).align_(\center);
		};
		oi2 = [oi1] ++ butt;
		oi2 = oi2.collect({|item, ind| [StaticText(mastOutWin)] ++ item });
		oi2.do{|item, index|
			item[0].font = basicFont;
			if(index == 0, {
				item[0].string_("O/S");
			}, {
				item[0].string_(index);
				item[0].align_(\center);
			});
			item[0].maxWidth_(15).maxHeight_(15).minWidth_(15).minHeight_(15);
			item[0].stringColor_(Color.white);
		};
		selButt = Button(mastOutWin).maxHeight_(15).font_(basicFont)
		.states_([["S E L E C T", Color.white, Color.black]]);
		selButt.srcAction = {
			mastOutArr = butt.collect({|item|  item.collect({|it| it.value }) });
			mastOutArr.postln;
			this.mapOutFunc;
			mastOutWin.close;
		};
		selButt.canFocus = false;
		if(mastOutArr.isNil, {
			mastOutArr = {0!oiOuts} ! oiIns;
			oiIns.collect{|item| item }.do{|it, ind| mastOutArr[ind][it] = 1 };
		});
		butt.flat.do{|item, index| item.value = mastOutArr.flat[index] };
		mastOutWin.layout = VLayout(*[HLayout(*oi2.collect { |x| VLayout(* x) }), selButt]);
		mastOutWin.front;
	}

	dirInRec {arg boundArr, scrollOrg;
		var states, view, butt, thisBounds;
		thisBounds = 	Rect(boundArr[0]+mixerWin.bounds.left - scrollOrg[0],
			(screenBounds.height-boundArr[1]-285)-(mixerWin.bounds.top-45),
			140, 240);
		(1..server.options.numInputBusChannels).pairsDo{|it1, it2|
			states = states.add(("Inputs " ++ it1 ++ "-" ++ it2))};
		if(recInputArr.isNil, {
			recInputArr = states.atAll([0]);
		});
		winDirRec = Window("", thisBounds, border: false);
		winDirRec.background_(Color.black);
		view = ListView(winDirRec,Rect(10,10,120,70))
		.items_(states)
		.background_(Color.clear)
		.font_(Font("Monaco", 10);)
		.stringColor_(Color.white)
		.hiliteColor_(colorMeter;)
		.selectionMode_(\multi);
		view.selection = recInputArr.collect({|item|
			states.indicesOfEqual(item) }).flat;
		butt = Button().maxHeight_(15).minHeight_(15)
		.states_([["", Color.white, Color.black]])
		.string_("S E L E C T")
		.font_(basicFont)
		.srcAction = {
			recInputArr = states.atAll(view.selection);
			winDirRec.close;
		};
		winDirRec.layout = VLayout(view, butt);
		winDirRec.front;
	}

	//source windows: an argument window for a track's source (a block), opened
	//from the source button under a track's input menu or with 'blkgui'
	sourceGUI {arg source, winLeft, winDown, trackNum;
		var ndefKey, blockIndex, liveBlock, blockName, specs, pairs, specArr,
		winName, srcWin, openWin, lastWin;
		ndefKey = if(source.isKindOf(NodeProxy), {source.key}, {source.asSymbol});
		openWin = this.prSourceWin(ndefKey);
		if(openWin.notNil, {
			if(trackNum.notNil, { this.prSrcWinTracks.put(trackNum, ndefKey) });
			openWin.front;
		}, {
			blockIndex = Block.ndefs.indexOf(Ndef(ndefKey));
			if(blockIndex.notNil, { liveBlock = Block.liveBlocks[blockIndex] });
			case
			{blockIndex.isNil} {
				("no source window for " ++ ndefKey).warn;
			}
			{liveBlock.isNil} {
				(ndefKey.asString ++ ": no block playing").warn;
			}
			{liveBlock[1] == \pattern} {
				(ndefKey.asString ++ ": no source window for patterns").warn;
			}
			{
				blockName = liveBlock[1];
				pairs = Ndef(ndefKey).getKeysValues;
				if(pairs.isEmpty, {
					(ndefKey.asString ++ ": " ++ blockName ++ " has no arguments").warn;
				}, {
					specs = SpecFile.read(\block, blockName, false) ? [];
					specArr = pairs.collect{|pair| specs.detect{|spec| spec[0] == pair[0] } };
					winName = ndefKey.asString ++ " | " ++ blockName.asString;
					if(winLeft.isNil or: { winDown.isNil }, {
						lastWin = (filtersWindow ? []) ++ (sourceWindows ? []);
						if(lastWin.isEmpty, {
							winLeft = 75; winDown = 75;
						}, {
							winLeft = lastWin.last.bounds.left + 10;
							winDown = lastWin.last.bounds.top + 10;
						});
					});
					srcWin = this.prArgWinGUI(ndefKey, winName, pairs, specArr,
						winLeft, winDown, "C L O S E   W I N D O W", {|win| win.close });
					sourceWindows = sourceWindows.add(srcWin);
					if(trackNum.notNil, { this.prSrcWinTracks.put(trackNum, ndefKey) });
					srcWin.onClose = {
						sourceWindows = sourceWindows.reject({|item| item.name == srcWin.name });
						this.prSrcWinTracks.keys.select({|key|
							this.prSrcWinTracks[key] == ndefKey }).do{|key|
							this.prSrcWinTracks.removeAt(key) };
					};
				});
			};
		});
	}

	prSrcWinTracks {
		srcWinTracks ?? { srcWinTracks = IdentityDictionary.new };
		^srcWinTracks;
	}

	prSourceWin {arg ndefKey;
		^(sourceWindows ? []).detect({|item| item.name.beginsWith(ndefKey.asString ++ " ") });
	}

	//rebuild an open source window, in the same place (new block, new mapping)
	updateSourceWin {arg ndefKey;
		{
			var win, bounds, trackNum;
			win = this.prSourceWin(ndefKey);
			if(win.notNil, {
				bounds = win.bounds;
				this.prSrcWinTracks.keysValuesDo{|key, value| if(value == ndefKey, {trackNum = key}) };
				win.close;
				this.sourceGUI(ndefKey, bounds.left, bounds.top, trackNum);
			});
		}.defer;
	}

	closeSourceWin {arg ndefKey;
		{
			var win;
			win = this.prSourceWin(ndefKey);
			if(win.notNil, { win.close });
		}.defer;
	}

	//label for a track's source button: the source shown in its input menu
	prSourceLabel {arg mixIndex;
		var labels;
		labels = this.inputLablesFunc(mixIndex, mixTrackNames);
		if(labels.notNil and: { labels[1].notNil }, {
			^labels[0][labels[1]].asString;
		}, {
			^"";
		});
	}

	prTrackSource {arg trackNum;
		var inTag, index;
		inTag = ("inTrack" ++ trackNum).asSymbol;
		if(inputs.notNil, { index = inputs.flop[0].indexOf(inTag) });
		^if(index.notNil, { inputs[index][1] }, { nil });
	}

	openTrackSource {arg trackNum=1, winLeft, winDown;
		var source;
		source = this.prTrackSource(trackNum);
		case
		{source.isNil} { ("track " ++ trackNum ++ " has no source").warn }
		{source.isArray} { ("track " ++ trackNum ++ " has more than one source").warn }
		{ this.sourceGUI(source, winLeft, winDown, trackNum) };
	}

	//called when a track's input changes: relabel its source button, and if
	//its source window is open, swap it for the new source's window
	prSourceChanged {arg trackNum, ndefsIn;
		var mixIndex, oldKey, newKey;
		{
			var win, bounds;
			if(mixTrackNames.notNil, {
				mixIndex = mixTrackNames.indexOf((\track ++ trackNum).asSymbol);
			});
			if(mixIndex.notNil and: { srcButtonArr.notNil }, {
				if(srcButtonArr[mixIndex].notNil and: { srcButtonArr[mixIndex].isClosed.not }, {
					srcButtonArr[mixIndex].string = this.prSourceLabel(mixIndex);
				});
			});
			oldKey = this.prSrcWinTracks[trackNum];
			if(oldKey.notNil, {
				if(ndefsIn.isKindOf(NodeProxy), { newKey = ndefsIn.key });
				if(newKey != oldKey, {
					win = this.prSourceWin(oldKey);
					this.prSrcWinTracks.removeAt(trackNum);
					if(win.notNil, {
						bounds = win.bounds;
						win.close;
						if(newKey.notNil, {
							this.sourceGUI(newKey, bounds.left, bounds.top, trackNum);
						});
					});
				});
			});
		}.defer;
	}

	updateFxWin {arg ndefKey;
		var convTag, getWin;
		if(filtersWindow.notNil, {
			if(filtersWindow.notEmpty, {
				getWin = filtersWindow.detect({|item, index|
					item.name.beginsWith(ndefKey.asString ++ " ") });
				if(getWin.notNil, {
					getWin.close;
				});
				server.sync;
				convTag = this.convFilterTag(ndefKey);
				this.filterGUI(convTag[0], convTag[1], convTag[2]);
			});
		});
	}

	nomixer {
		if(mixerWin.notNil, {
			{mixerWin.close}.defer;
		});
	}
}
