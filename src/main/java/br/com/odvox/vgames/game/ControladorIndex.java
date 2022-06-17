package br.com.odvox.vgames.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.util.IOUtils;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

@ManagedBean
@RequestScoped
public class ControladorIndex implements Serializable {

	/**
	 * SERIAL.
	 */
	private static final long serialVersionUID = -8171466470858370914L;
	

	public ControladorIndex() {

	}

	@PostConstruct
	public void setUrls() {

		String advertencia = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("advertencia");
		String locutor = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("locutor");
		Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
		try {
			if ((null != advertencia) && (null != locutor)) {
				this.setUrlAdvertencia(this.getVozURL(advertencia, locutor, locale));
				FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	private String urlAdvertencia;

	/**
	 * @return the urlAdvertencia
	 */
	public String getUrlAdvertencia() {

		return this.urlAdvertencia;
	}

	/**
	 * @param urlAdvertencia
	 *            the urlAdvertencia to set
	 */
	public void setUrlAdvertencia(String urlAdvertencia) {

		this.urlAdvertencia = urlAdvertencia;
	}

	public String getVozURL(final String texto, String locutor, Locale locale) throws UnsupportedEncodingException {
		final AmazonPollyClient polly;
		final Voice voice;
		Region region = Region.getRegion(Regions.AP_SOUTHEAST_1);
		// create an Amazon Polly client in a specific region
		polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(), 
		new ClientConfiguration());
		polly.setRegion(region);
		// Create describe voices request.
		DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

		// Synchronously ask Amazon Polly to describe available TTS voices.
		DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
		voice = describeVoicesResult.getVoices().get(7);

		InputStream speechStream = null;
		try {
			speechStream = synthesize(texto, OutputFormat.Mp3, voice, polly);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//create an MP3 player
		AdvancedPlayer player = null;
		try {
			player = new AdvancedPlayer(speechStream,
					javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		player.setPlayBackListener(new PlaybackListener() {
			@Override
			public void playbackStarted(PlaybackEvent evt) {
				System.out.println("Playback started");
				System.out.println(texto);
			}
			
			@Override
			public void playbackFinished(PlaybackEvent evt) {
				System.out.println("Playback finished");
			}
		});
		
		
		// play it!
		try {
			player.play();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			byte[] bytes = IOUtils.toByteArray(speechStream);
			String vozBase64 = Base64.getEncoder().encodeToString(bytes);
			return "data:audio/mp3;base64," + vozBase64;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public InputStream synthesize(String text, OutputFormat format, Voice voice, AmazonPollyClient polly) throws IOException {
		SynthesizeSpeechRequest synthReq = 
		new SynthesizeSpeechRequest().withText(text).withVoiceId(voice.getId())
				.withOutputFormat(format);
		SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);

		return synthRes.getAudioStream();
	}
}
